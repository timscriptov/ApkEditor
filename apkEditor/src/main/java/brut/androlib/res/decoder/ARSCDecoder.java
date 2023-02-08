/**
 * Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package brut.androlib.res.decoder;

import android.util.Log;
import android.util.TypedValue;

import com.google.common.io.LittleEndianDataInputStream;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.CountingInputStream;
import brut.androlib.res.data.ResConfigFlags;
import brut.androlib.res.data.ResID;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.ResType;
import brut.androlib.res.data.ResTypeSpec;
import brut.androlib.res.data.value.ResBagValue;
import brut.androlib.res.data.value.ResBoolValue;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.data.value.ResIntBasedValue;
import brut.androlib.res.data.value.ResScalarValue;
import brut.androlib.res.data.value.ResStringValue;
import brut.androlib.res.data.value.ResValue;
import brut.androlib.res.data.value.ResValueFactory;
import brut.util.Duo;
import brut.util.ExtDataInput;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
// Modified a lot by Pujiang
public class ARSCDecoder {
    private final static short ENTRY_FLAG_COMPLEX = 0x0001;
    private final static short ENTRY_FLAG_PUBLIC = 0x0002;
    private final static short ENTRY_FLAG_WEAK = 0x0004;
    private static final Logger LOGGER = Logger.getLogger(ARSCDecoder.class.getName());
    private static final int KNOWN_CONFIG_BYTES = 56;
    private final ExtDataInput mIn;
    private final ResTable mResTable;
    private final CountingInputStream mCountIn;
    private final List<FlagsOffset> mFlagsOffsets;
    private final boolean mKeepBroken;
    // Resource id provider, most likely from R.smali
    private ResourceIdProvider idProvider;
    private boolean[] mIsFileValues;
    private boolean mIsFileValue;
    private boolean mApkProtected;
    // readTableTypeSpec call readTableType
    private byte[] perfBuf = new byte[12];
    private Header mHeader;
    private StringBlock mTableStrings;
    private StringBlock mTypeNames;
    private StringBlock mSpecNames;
    private ResPackage mPkg;
    private ResTypeSpec mTypeSpec;
    private ResType mType;
    private int mResId;
    private int mTypeIdOffset = 0;
    private boolean[] mMissingResSpecs;
    private HashMap<Integer, ResTypeSpec> mResTypeSpecs = new HashMap<>();

    private ARSCDecoder(InputStream arscStream, ResTable resTable, ResourceIdProvider idProvider, boolean storeFlagsOffsets, boolean keepBroken, boolean apkProtected) {
        this.idProvider = idProvider;
        this.mApkProtected = apkProtected;
        arscStream = mCountIn = new CountingInputStream(arscStream);
        if (storeFlagsOffsets) {
            mFlagsOffsets = new ArrayList<FlagsOffset>();
        } else {
            mFlagsOffsets = null;
        }
        // We need to explicitly cast to DataInput as otherwise the constructor is ambiguous.
        // We choose DataInput instead of InputStream as ExtDataInput wraps an InputStream in
        // a DataInputStream which is big-endian and ignores the little-endian behavior.
        mIn = new ExtDataInput((DataInput) new LittleEndianDataInputStream(arscStream));
        mResTable = resTable;
        mKeepBroken = keepBroken;
    }

    public static ARSCData decode(InputStream arscStream, boolean findFlagsOffsets, boolean keepBroken)
            throws AndrolibException {
        return decode(arscStream, findFlagsOffsets, keepBroken, new ResTable(), null, false);
    }

    public static ARSCData decode(InputStream arscStream, boolean findFlagsOffsets, boolean keepBroken,
                                  ResTable resTable, ResourceIdProvider idProvider, boolean apkProtected)
            throws AndrolibException {
        try {
            ARSCDecoder decoder = new ARSCDecoder(
                    arscStream, resTable, idProvider, findFlagsOffsets, keepBroken, apkProtected);
            //TimeDumper timer = new TimeDumper(true);
            ResPackage[] pkgs = decoder.readTableHeader();
            //timer.lastTime("Decode Time");
            return new ARSCData(pkgs, decoder.mFlagsOffsets == null
                    ? null
                    : decoder.mFlagsOffsets.toArray(new FlagsOffset[0]), resTable);
        } catch (IOException ex) {
            throw new AndrolibException("Could not decode arsc file", ex);
        }
    }

    private ResPackage[] readTableHeader() throws IOException, AndrolibException {
        nextChunkCheckType(Header.TYPE_TABLE);
        int packageCount = mIn.readInt();

        mTableStrings = StringBlock.read(mIn);
        //dumpStrings(mTableStrings);
        ResPackage[] packages = new ResPackage[packageCount];

        nextChunk();
        // Nearly all time consumed in following code
        for (int i = 0; i < packageCount; i++) {
            packages[i] = readTablePackage();
        }
        return packages;
    }

    private void dumpStrings(StringBlock strBlock) {
//        int count = strBlock.getCount();
//        Log.d("DEBUG", "count=" + count);
//        for (int i = 0; i < count; i++) {
//            String v = strBlock.getString(i);
//            if (v.equals("WhatsApp")) {
//                Log.d("DEBUG", "string[" + i + "]=" + v);
//            }
//        }
    }

    private ResPackage readTablePackage() throws IOException, AndrolibException {
        checkChunkType(Header.TYPE_PACKAGE);
        int id = mIn.readInt();

        if (id == 0) {
            // This means we are dealing with a Library Package, we should just temporarily
            // set the packageId to the next available id . This will be set at runtime regardless, but
            // for Apktool's use we need a non-zero packageId.
            // AOSP indicates 0x02 is next, as 0x01 is system and 0x7F is private.
            id = 2;
            if (mResTable.getPackageOriginal() == null && mResTable.getPackageRenamed() == null) {
                mResTable.setSharedLibrary(true);
            }
        }

        String name = mIn.readNullEndedString(128, true);
        /* typeStrings */
        mIn.skipInt();
        /* lastPublicType */
        mIn.skipInt();
        /* keyStrings */
        mIn.skipInt();
        /* lastPublicKey */
        mIn.skipInt();

        mTypeNames = StringBlock.read(mIn);
        mSpecNames = StringBlock.read(mIn);

        // Added by Pujiang to revise the invalid name to _rr
        if (mApkProtected) {
            int invalidIdx = 0;
            StringBuilder sb = new StringBuilder();
            sb.append('_');
            sb.append('r');
            sb.append('r');
            String header = sb.toString();
            for (int i = 0; i < mSpecNames.getCount(); ++i) {
                String specName = mSpecNames.getString(i);
                // Not empty but invalid
                if (specName != null && !specName.equals("") &&
                        !specName.matches("[a-zA-Z0-9_\\.]+")) {
                    //Log.d("DEBUG", "rename " + specName + " to " + header + invalidIdx);
                    mSpecNames.setString(i, header + invalidIdx);
                    invalidIdx += 1;
                } else if ("do".equals(specName)) {
                    mSpecNames.setString(i, "do" + header + invalidIdx);
                    invalidIdx += 1;
                } else if ("if".equals(specName)) {
                    mSpecNames.setString(i, "if" + header + invalidIdx);
                    invalidIdx += 1;
                } else if ("for".equals(specName)) {
                    mSpecNames.setString(i, "for" + header + invalidIdx);
                    invalidIdx += 1;
                }
            }
        }

        // Added by Pujiang
        this.mIsFileValues = new boolean[mTypeNames.getCount() + 32];
        Arrays.fill(mIsFileValues, false);
        for (int i = 0; i < mTypeNames.getCount(); i++) {
            String curName = mTypeNames.getString(i);
            if (curName.equals("ani") || curName.equals("animator")
                    || curName.equals("drawable") || curName.equals("interpolator")
                    || curName.equals("layout") || curName.equals("menu")
                    || curName.equals("mipmap") || curName.equals("raw")
                    || curName.equals("transition") || curName.equals("xml")) {
                mIsFileValues[i] = true;
            }
        }

        mResId = id << 24;
        mPkg = new ResPackage(mResTable, id, name);

        nextChunk();
        while (mHeader.type == Header.TYPE_LIBRARY) {
            readLibraryType();
        }

        // Nearly all the time consumed here
        while (mHeader.type == Header.TYPE_SPEC_TYPE) {
            readTableTypeSpec();
        }

        return mPkg;
    }

    private void readLibraryType() throws AndrolibException, IOException {
        checkChunkType(Header.TYPE_LIBRARY);
        int libraryCount = mIn.readInt();

        int packageId;
        String packageName;

        for (int i = 0; i < libraryCount; i++) {
            packageId = mIn.readInt();
            packageName = mIn.readNullEndedString(128, true);
            LOGGER.info(String.format("Decoding (%s), pkgId: %d", packageName, packageId));
        }

        while (nextChunk().type == Header.TYPE_TYPE) {
            readTableTypeSpec();
        }
    }

    private ResTypeSpec readTableTypeSpec() throws AndrolibException, IOException {
        mTypeSpec = readSingleTableTypeSpec();
        addTypeSpec(mTypeSpec);

        int type = nextChunk().type;
        ResTypeSpec resTypeSpec;

        while (type == Header.TYPE_SPEC_TYPE) {
            resTypeSpec = readSingleTableTypeSpec();
            addTypeSpec(resTypeSpec);
            type = nextChunk().type;

            if (!mResTable.getSparseResources()) {
                mResTable.setSparseResources(true);
            }
        }

        // Nearly all time consumed here
        while (type == Header.TYPE_TYPE) {
            readTableType();

            // skip "TYPE 8 chunks" and/or padding data at the end of this chunk
            if (mCountIn.getCount() < mHeader.endPosition) {
                mCountIn.skip(mHeader.endPosition - mCountIn.getCount());
            }

            type = nextChunk().type;

            addMissingResSpecs();
        }

        return mTypeSpec;
    }

    private ResTypeSpec readSingleTableTypeSpec() throws AndrolibException, IOException {
        checkChunkType(Header.TYPE_SPEC_TYPE);
        int id = mIn.readUnsignedByte();
        mIn.skipBytes(3);
        int entryCount = mIn.readInt();

        if (mFlagsOffsets != null) {
            mFlagsOffsets.add(new FlagsOffset(mCountIn.getCount(), entryCount));
        }

        /* flags */
        mIn.skipBytes(entryCount * 4);
        mTypeSpec = new ResTypeSpec(mTypeNames.getString(id - 1), mResTable, mPkg, id, entryCount);
        mPkg.addType(mTypeSpec);
        return mTypeSpec;
    }

    private ResType readTableType() throws IOException, AndrolibException {
        checkChunkType(Header.TYPE_TYPE);
        //byte typeId = mIn.readByte();
        mIn.readFully(perfBuf);
        int typeId = perfBuf[0] & 0xff;
        if (mResTypeSpecs.containsKey(typeId)) {
            mResId = (0xff000000 & mResId) | mResTypeSpecs.get(typeId).getId() << 16;
            mTypeSpec = mResTypeSpecs.get(typeId);
        }
        //   int typeFlags = mIn.readByte();
        // Added by Pujiang
        if (typeId < mIsFileValues.length) {
            mIsFileValue = this.mIsFileValues[typeId - 1];
        } else {
            mIsFileValue = false;
        }

        // /* res0, res1 */mIn.skipBytes(3);
        // int entryCount = mIn.readInt();
        // int entriesStart = mIn.readInt();
        int entryCount = (perfBuf[7] & 0xff) << 24 | (perfBuf[6] & 0xff) << 16
                | (perfBuf[5] & 0xff) << 8 | (perfBuf[4] & 0xff);
        int entriesStart = (perfBuf[11] & 0xff) << 24 | (perfBuf[10] & 0xff) << 16
                | (perfBuf[9] & 0xff) << 8 | (perfBuf[8] & 0xff);
        //Log.d("DEBUG", "\ttypeId=" + typeId + ", entryCount=" + entryCount);

        mMissingResSpecs = new boolean[entryCount];
        Arrays.fill(mMissingResSpecs, true);

        ResConfigFlags flags = readConfigFlags();
        int position = (mHeader.startPosition + entriesStart) - (entryCount * 4);

        // For some APKs there is a disconnect between the reported size of Configs
        // If we find a mismatch skip those bytes.
        if (position != mCountIn.getCount()) {
            mIn.skipBytes(position - mCountIn.getCount());
        }

//        if (typeFlags == 1) {
//            LOGGER.warning("Sparse type flags detected: " + mTypeSpec.getName());
//        }
        int[] entryOffsets = mIn.readIntArray(entryCount);

        if (flags.isInvalid) {
            String resName = mTypeSpec.getName() + flags.getQualifiers();
            if (mKeepBroken) {
                LOGGER.warning("Invalid config flags detected: " + resName);
            } else {
                LOGGER.warning("Invalid config flags detected. Dropping resources: " + resName);
            }
        }

        mType = flags.isInvalid && !mKeepBroken ? null : mPkg.getOrCreateConfig(flags);
        HashMap<Integer, EntryData> offsetsToEntryData = new HashMap<Integer, EntryData>();

        for (int offset : entryOffsets) {
            if (offset == -1 || offsetsToEntryData.containsKey(offset)) {
                continue;
            }

            offsetsToEntryData.put(offset, readEntryData());
        }

        for (int i = 0; i < entryOffsets.length; i++) {
            if (entryOffsets[i] != -1) {
                mMissingResSpecs[i] = false;
                mResId = (mResId & 0xffff0000) | i;
                EntryData entryData = offsetsToEntryData.get(entryOffsets[i]);
                readEntry(entryData);
            }
        }

        return mType;
    }

    private EntryData readEntryData() throws IOException, AndrolibException {
        short size = mIn.readShort();
        if (size < 0) {
            throw new AndrolibException("Entry size is under 0 bytes.");
        }

        short flags = mIn.readShort();
        int specNamesId = mIn.readInt();
        ResValue value = (flags & ENTRY_FLAG_COMPLEX) == 0 ? readValue() : readComplexEntry();
        EntryData entryData = new EntryData();
        entryData.mFlags = flags;
        entryData.mSpecNamesId = specNamesId;
        entryData.mValue = value;
        return entryData;
    }

    // Pujiang: Revise the name if we can get it from other providers (like R.smali)
    private String getResName(int resId, int specNamesId) {
        String name = null;
        if (idProvider == null) {
            name = mSpecNames.getString(specNamesId);
        } else {
            name = idProvider.getNameById(resId);
            if (name == null) {
                name = mSpecNames.getString(specNamesId);
            }
        }
        return name;
    }

    private void readEntry(EntryData entryData) throws AndrolibException {
        int specNamesId = entryData.mSpecNamesId;
        ResValue value = entryData.mValue;

        if (mTypeSpec.isString() && value instanceof ResFileValue) {
            value = new ResStringValue(value.toString(), ((ResFileValue) value).getRawIntValue());
        }
        if (mType == null) {
            return;
        }

        ResID resId = new ResID(mResId);
        ResResSpec spec;
        if (mPkg.hasResSpec(resId)) {
            spec = mPkg.getResSpec(resId);

            if (spec.isDummyResSpec()) {
                removeResSpec(spec);

                //spec = new ResResSpec(resId, mSpecNames.getString(specNamesId), mPkg, mTypeSpec);
                spec = new ResResSpec(resId, getResName(resId.id, specNamesId), mPkg, mTypeSpec, mApkProtected);
                mPkg.addResSpec(spec);
                mTypeSpec.addResSpec(spec);
            }
        } else {
            //spec = new ResResSpec(resId, mSpecNames.getString(specNamesId), mPkg, mTypeSpec);
            spec = new ResResSpec(resId, getResName(resId.id, specNamesId), mPkg, mTypeSpec, mApkProtected);
            mPkg.addResSpec(spec);
            mTypeSpec.addResSpec(spec);
        }
        ResResource res = new ResResource(mType, spec, value);

        try {
            mType.addResource(res);
            spec.addResource(res);
        } catch (AndrolibException ex) {
            if (mKeepBroken) {
                mType.addResource(res, true);
                spec.addResource(res, true);
                LOGGER.warning(String.format("Duplicate res igonred: %s", res.toString()));
            } else {
                throw ex;
            }
        }
        mPkg.addResource(res);
    }

    private ResBagValue readComplexEntry() throws IOException, AndrolibException {
//        int parent = mIn.readInt();
//        int count = mIn.readInt();
        int head[] = mIn.readIntArray(2);
        int parent = head[0];
        int count = head[1];

        ResValueFactory factory = mPkg.getValueFactory();
        Duo<Integer, ResScalarValue>[] items = new Duo[count];
        ResIntBasedValue resValue;
        int resId;

        byte[] raw = new byte[(4 + 8) * count];
        mIn.readFully(raw);
        for (int i = 0; i < count; i++) {
//            resId = mIn.readInt();
//            resValue = readValue();
            int offset = i * 12;
            resId = (raw[offset + 3] & 0xff) << 24 | (raw[offset + 2] & 0xff) << 16
                    | (raw[offset + 1] & 0xff) << 8 | (raw[offset] & 0xff);
            byte type = raw[offset + 7];
            int data = (raw[offset + 11] & 0xff) << 24 | (raw[offset + 10] & 0xff) << 16
                    | (raw[offset + 9] & 0xff) << 8 | (raw[offset + 8] & 0xff);
            if (((raw[offset + 5] & 0xff) << 8 | (raw[offset + 4] & 0xff)) != 8) {
                Log.d("DEBUG", "error");
            }
            resValue = readValue(type, data);

            if (resValue instanceof ResScalarValue) {
                items[i] = new Duo<Integer, ResScalarValue>(resId, (ResScalarValue) resValue);
            } else {
                resValue = new ResStringValue(resValue.toString(), resValue.getRawIntValue());
                items[i] = new Duo<Integer, ResScalarValue>(resId, (ResScalarValue) resValue);
            }
        }

        return factory.bagFactory(parent, items, mTypeSpec);
    }

    private ResIntBasedValue readValue(byte type, int data) throws IOException, AndrolibException {
//		/* size */mIn.skipCheckShort((short) 8);
//		/* zero */mIn.skipCheckByte((byte) 0);
//        byte type = mIn.readByte();
//        int data = mIn.readInt();

        // Modified by pujiang to get better perf
        boolean isStyled = false;
        String rawStr = "";
        String value = "";
        if ((mResId & 0xff000000) != 0x1000000) {
            rawStr = mTableStrings.getString(data);
            value = mTableStrings.getHTML(data, rawStr);
            isStyled = mTableStrings.isStyledString();
        }

        return type == TypedValue.TYPE_STRING
                ? mPkg.getValueFactory().factory(value, data, mIsFileValue, rawStr, isStyled)
                : mPkg.getValueFactory().factory(type, data, null);
    }

    private ResIntBasedValue readValue() throws IOException, AndrolibException {
//      /* size */mIn.skipCheckShort((short) 8);
//      /* zero */mIn.skipCheckByte((byte) 0);
//        byte type = mIn.readByte();
//        int data = mIn.readInt();

        byte[] raw = new byte[8];
        mIn.readFully(raw);
        byte type = raw[3];
        int data = (raw[7] & 0xff) << 24 | (raw[6] & 0xff) << 16
                | (raw[5] & 0xff) << 8 | (raw[4] & 0xff);

        return readValue(type, data);
        // Modified by pujiang to get better perf
//        return type == TypedValue.TYPE_STRING
//                ? mPkg.getValueFactory().factory((mResId & 0xff000000) == 0x1000000 ? "" : mTableStrings.getHTML(data), data, mIsFileValue)
//                : mPkg.getValueFactory().factory(type, data, null);
    }

    private ResConfigFlags readConfigFlags() throws IOException, AndrolibException {
        int size = mIn.readInt();
        int read = 28;

        if (size < 28) {
            throw new AndrolibException("Config size < 28");
        }

        boolean isInvalid = false;

        int index = 0;
        byte[] raw = new byte[24];
        mIn.readFully(raw);

//        short mcc = mIn.readShort();
//        short mnc = mIn.readShort();
        short mcc = (short) ((raw[index + 1] & 0xff) << 8 | (raw[index] & 0xff));
        index += 2;
        short mnc = (short) ((raw[index + 1] & 0xff) << 8 | (raw[index] & 0xff));
        index += 2;

//        char[] language = this.unpackLanguageOrRegion(mIn.readByte(), mIn.readByte(), 'a');
//        char[] country = this.unpackLanguageOrRegion(mIn.readByte(), mIn.readByte(), '0');
        char[] language = this.unpackLanguageOrRegion(raw[index++], raw[index++], 'a');
        char[] country = this.unpackLanguageOrRegion(raw[index++], raw[index++], '0');

//        byte orientation = mIn.readByte();
//        byte touchscreen = mIn.readByte();
        byte orientation = raw[index++];
        byte touchscreen = raw[index++];

//        int density = mIn.readUnsignedShort();
        int density = (raw[index + 1] & 0xff) << 8 | (raw[index] & 0xff);
        index += 2;

//        byte keyboard = mIn.readByte();
//        byte navigation = mIn.readByte();
//        byte inputFlags = mIn.readByte();
//		/* inputPad0 */mIn.skipBytes(1);
        byte keyboard = raw[index++];
        byte navigation = raw[index++];
        byte inputFlags = raw[index++];
        index++;

//        short screenWidth = mIn.readShort();
//        short screenHeight = mIn.readShort();
        short screenWidth = (short) ((raw[index + 1] & 0xff) << 8 | (raw[index] & 0xff));
        index += 2;
        short screenHeight = (short) ((raw[index + 1] & 0xff) << 8 | (raw[index] & 0xff));
        index += 2;

//        short sdkVersion = mIn.readShort();
//		/* minorVersion, now must always be 0 */mIn.skipBytes(2);
        short sdkVersion = (short) ((raw[index + 1] & 0xff) << 8 | (raw[index] & 0xff));
        index += 2;
        /* minorVersion, now must always be 0 */
        index += 2;

        byte screenLayout = 0;
        byte uiMode = 0;
        short smallestScreenWidthDp = 0;
        if (size >= 32) {
//            screenLayout = mIn.readByte();
//            uiMode = mIn.readByte();
//            smallestScreenWidthDp = mIn.readShort();
            byte[] buf = new byte[4];
            mIn.readFully(buf);
            screenLayout = buf[0];
            uiMode = buf[1];
            smallestScreenWidthDp = (short) ((buf[3] & 0xff) << 8 | (buf[2] & 0xff));
            read = 32;
        }

        short screenWidthDp = 0;
        short screenHeightDp = 0;
        if (size >= 36) {
//            screenWidthDp = mIn.readShort();
//            screenHeightDp = mIn.readShort();
            byte[] buf = new byte[4];
            mIn.readFully(buf);
            screenWidthDp = (short) ((buf[1] & 0xff) << 8 | (buf[0] & 0xff));
            screenHeightDp = (short) ((buf[3] & 0xff) << 8 | (buf[2] & 0xff));
            read = 36;
        }

        char[] localeScript = null;
        char[] localeVariant = null;
        if (size >= 48) {
            localeScript = readScriptOrVariantChar(4).toCharArray();
            localeVariant = readScriptOrVariantChar(8).toCharArray();
            read = 48;
        }

        byte screenLayout2 = 0;
        byte colorMode = 0;
        if (size >= 52) {
            screenLayout2 = mIn.readByte();
            colorMode = mIn.readByte();
            mIn.skipBytes(2); // reserved padding
            read = 52;
        }

        if (size >= 56) {
            mIn.skipBytes(4);
            read = 56;
        }

        int exceedingSize = size - KNOWN_CONFIG_BYTES;
        if (exceedingSize > 0) {
            byte[] buf = new byte[exceedingSize];
            read += exceedingSize;
            mIn.readFully(buf);
            BigInteger exceedingBI = new BigInteger(1, buf);

            if (exceedingBI.equals(BigInteger.ZERO)) {
                //LOGGER.fine(String
                //        .format("Config flags size > %d, but exceeding bytes are all zero, so it should be ok.",
                //                KNOWN_CONFIG_BYTES));
            } else {
                //LOGGER.warning(String.format("Config flags size > %d. Exceeding bytes: 0x%X.",
                //        KNOWN_CONFIG_BYTES, exceedingBI));
                isInvalid = true;
            }
        }

        int remainingSize = size - read;
        if (remainingSize > 0) {
            mIn.skipBytes(remainingSize);
        }

        return new ResConfigFlags(mcc, mnc, language, country,
                orientation, touchscreen, density, keyboard, navigation,
                inputFlags, screenWidth, screenHeight, sdkVersion,
                screenLayout, uiMode, smallestScreenWidthDp, screenWidthDp,
                screenHeightDp, localeScript, localeVariant, screenLayout2,
                colorMode, isInvalid, size);
    }

    private char[] unpackLanguageOrRegion(byte in0, byte in1, char base) throws AndrolibException {
        // check high bit, if so we have a packed 3 letter code
        if (((in0 >> 7) & 1) == 1) {
            int first = in1 & 0x1F;
            int second = ((in1 & 0xE0) >> 5) + ((in0 & 0x03) << 3);
            int third = (in0 & 0x7C) >> 2;

            // since this function handles languages & regions, we add the value(s) to the base char
            // which is usually 'a' or '0' depending on language or region.
            return new char[]{(char) (first + base), (char) (second + base), (char) (third + base)};
        }
        return new char[]{(char) in0, (char) in1};
    }

    private String readScriptOrVariantChar(int length) throws AndrolibException, IOException {
        StringBuilder string = new StringBuilder(16);

        while (length-- != 0) {
            short ch = mIn.readByte();
            if (ch == 0) {
                break;
            }
            string.append((char) ch);
        }
        mIn.skipBytes(length);

        return string.toString();
    }

    private void addTypeSpec(ResTypeSpec resTypeSpec) {
        mResTypeSpecs.put(resTypeSpec.getId(), resTypeSpec);
    }

    private void addMissingResSpecs() throws AndrolibException {
        int resId = mResId & 0xffff0000;

        for (int i = 0; i < mMissingResSpecs.length; i++) {
            if (!mMissingResSpecs[i]) {
                continue;
            }

            ResResSpec spec = new ResResSpec(new ResID(resId | i), "dummy_ae_" + Integer.toHexString(i), mPkg, mTypeSpec, false);

            // If we already have this resID dont add it again.
            if (!mPkg.hasResSpec(new ResID(resId | i))) {
                mPkg.addResSpec(spec);
                mTypeSpec.addResSpec(spec);

                if (mType == null) {
                    mType = mPkg.getOrCreateConfig(new ResConfigFlags());
                }

                ResValue value = new ResBoolValue(false, 0, null);
                ResResource res = new ResResource(mType, spec, value);

                mPkg.addResource(res);
                mType.addResource(res);
                spec.addResource(res);
            }
        }
    }

    private void removeResSpec(ResResSpec spec) throws AndrolibException {
        if (mPkg.hasResSpec(spec.getId())) {
            mPkg.removeResSpec(spec);
            mTypeSpec.removeResSpec(spec);
        }
    }

    private Header nextChunk() throws IOException {
        return mHeader = Header.read(mIn, mCountIn);
    }

    private void checkChunkType(int expectedType) throws AndrolibException {
        if (mHeader.type != expectedType) {
            throw new AndrolibException(String.format("Invalid chunk type: expected=0x%08x, got=0x%08x",
                    expectedType, mHeader.type));
        }
    }

    private void nextChunkCheckType(int expectedType) throws IOException, AndrolibException {
        nextChunk();
        checkChunkType(expectedType);
    }

    public static class Header {
        public final static short TYPE_NONE = -1, TYPE_TABLE = 0x0002,
                TYPE_PACKAGE = 0x0200, TYPE_TYPE = 0x0201, TYPE_SPEC_TYPE = 0x0202, TYPE_LIBRARY = 0x0203;
        public final short type;
        public final int headerSize;
        public final int chunkSize;
        public final int startPosition;
        public final int endPosition;

        public Header(short type, int headerSize, int chunkSize, int headerStart) {
            this.type = type;
            this.headerSize = headerSize;
            this.chunkSize = chunkSize;
            this.startPosition = headerStart;
            this.endPosition = headerStart + chunkSize;
        }

        public static Header read(ExtDataInput in, CountingInputStream countIn) throws IOException {
            short type;
            int start = countIn.getCount();
            try {
                type = in.readShort();
            } catch (EOFException ex) {
                return new Header(TYPE_NONE, 0, 0, countIn.getCount());
            }
            return new Header(type, in.readShort(), in.readInt(), start);
        }
    }

    public static class FlagsOffset {
        public final int offset;
        public final int count;

        public FlagsOffset(int offset, int count) {
            this.offset = offset;
            this.count = count;
        }
    }

    public static class ARSCData {

        private final ResPackage[] mPackages;
        private final FlagsOffset[] mFlagsOffsets;
        private final ResTable mResTable;

        public ARSCData(ResPackage[] packages, FlagsOffset[] flagsOffsets, ResTable resTable) {
            mPackages = packages;
            mFlagsOffsets = flagsOffsets;
            mResTable = resTable;
        }

        public FlagsOffset[] getFlagsOffsets() {
            return mFlagsOffsets;
        }

        public ResPackage[] getPackages() {
            return mPackages;
        }

        public ResPackage getOnePackage() throws AndrolibException {
            if (mPackages.length <= 0) {
                throw new AndrolibException("Arsc file contains zero packages");
            } else if (mPackages.length != 1) {
                int id = findPackageWithMostResSpecs();
                LOGGER.info("Arsc file contains multiple packages. Using package "
                        + mPackages[id].getName() + " as default.");

                return mPackages[id];
            }
            return mPackages[0];
        }

        public int findPackageWithMostResSpecs() {
            int count = mPackages[0].getResSpecCount();
            int id = 0;

            for (int i = 0; i < mPackages.length; i++) {
                if (mPackages[i].getResSpecCount() >= count) {
                    count = mPackages[i].getResSpecCount();
                    id = i;
                }
            }
            return id;
        }

        public ResTable getResTable() {
            return mResTable;
        }
    }

    private class EntryData {
        public short mFlags;
        public int mSpecNamesId;
        public ResValue mValue;
    }
}

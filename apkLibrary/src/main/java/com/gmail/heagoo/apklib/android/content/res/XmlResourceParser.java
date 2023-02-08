/*
 * Copyright 2008 Android4ME
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gmail.heagoo.apklib.android.content.res;

import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * @author Dmitry Skiba
 */
public interface XmlResourceParser extends XmlPullParser, AttributeSet {
    void close();

    @Override
    default int getAttributeNameResource(int index) {
        return 0;
    }

    @Override
    default int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue) {
        return 0;
    }

    @Override
    default boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue) {
        return false;
    }

    @Override
    default int getAttributeResourceValue(String namespace, String attribute, int defaultValue) {
        return 0;
    }

    @Override
    default int getAttributeIntValue(String namespace, String attribute, int defaultValue) {
        return 0;
    }

    @Override
    default int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue) {
        return 0;
    }

    @Override
    default float getAttributeFloatValue(String namespace, String attribute, float defaultValue) {
        return 0;
    }

    @Override
    default int getAttributeListValue(int index, String[] options, int defaultValue) {
        return 0;
    }

    @Override
    default boolean getAttributeBooleanValue(int index, boolean defaultValue) {
        return false;
    }

    @Override
    default int getAttributeResourceValue(int index, int defaultValue) {
        return 0;
    }

    @Override
    default int getAttributeIntValue(int index, int defaultValue) {
        return 0;
    }

    @Override
    default int getAttributeUnsignedIntValue(int index, int defaultValue) {
        return 0;
    }

    @Override
    default float getAttributeFloatValue(int index, float defaultValue) {
        return 0;
    }

    @Override
    default String getIdAttribute() {
        return null;
    }

    @Override
    default String getClassAttribute() {
        return null;
    }

    @Override
    default int getIdAttributeResourceValue(int defaultValue) {
        return 0;
    }

    @Override
    default int getStyleAttribute() {
        return 0;
    }

    @Override
    default void setFeature(String name, boolean state) throws XmlPullParserException {

    }

    @Override
    default boolean getFeature(String name) {
        return false;
    }

    @Override
    default void setProperty(String name, Object value) throws XmlPullParserException {

    }

    @Override
    default Object getProperty(String name) {
        return null;
    }

    @Override
    default void setInput(Reader in) throws XmlPullParserException {

    }

    @Override
    default void setInput(InputStream inputStream, String inputEncoding) throws XmlPullParserException {

    }

    @Override
    default String getInputEncoding() {
        return null;
    }

    @Override
    default void defineEntityReplacementText(String entityName, String replacementText) throws XmlPullParserException {

    }

    @Override
    default int getNamespaceCount(int depth) throws XmlPullParserException {
        return 0;
    }

    @Override
    default String getNamespacePrefix(int pos) throws XmlPullParserException {
        return null;
    }

    @Override
    default String getNamespaceUri(int pos) throws XmlPullParserException {
        return null;
    }

    @Override
    default String getNamespace(String prefix) {
        return null;
    }

    @Override
    default int getDepth() {
        return 0;
    }

    @Override
    default String getPositionDescription() {
        return null;
    }

    @Override
    default int getLineNumber() {
        return 0;
    }

    @Override
    default int getColumnNumber() {
        return 0;
    }

    @Override
    default boolean isWhitespace() throws XmlPullParserException {
        return false;
    }

    @Override
    default String getText() {
        return null;
    }

    @Override
    default char[] getTextCharacters(int[] holderForStartAndLength) {
        return new char[0];
    }

    @Override
    default String getNamespace() {
        return null;
    }

    @Override
    default String getName() {
        return null;
    }

    @Override
    default String getPrefix() {
        return null;
    }

    @Override
    default boolean isEmptyElementTag() throws XmlPullParserException {
        return false;
    }

    @Override
    default int getAttributeCount() {
        return 0;
    }

    @Override
    default String getAttributeNamespace(int index) {
        return null;
    }

    @Override
    default String getAttributeName(int index) {
        return null;
    }

    @Override
    default String getAttributePrefix(int index) {
        return null;
    }

    @Override
    default String getAttributeType(int index) {
        return null;
    }

    @Override
    default boolean isAttributeDefault(int index) {
        return false;
    }

    @Override
    default String getAttributeValue(int index) {
        return null;
    }

    @Override
    default String getAttributeValue(String namespace, String name) {
        return null;
    }

    @Override
    default int getEventType() throws XmlPullParserException {
        return 0;
    }

    @Override
    default int next() throws IOException, XmlPullParserException {
        return 0;
    }

    @Override
    default int nextToken() throws IOException, XmlPullParserException {
        return 0;
    }

    @Override
    default void require(int type, String namespace, String name) throws IOException, XmlPullParserException {

    }

    @Override
    default String nextText() throws IOException, XmlPullParserException {
        return null;
    }

    @Override
    default int nextTag() throws IOException, XmlPullParserException {
        return 0;
    }
}

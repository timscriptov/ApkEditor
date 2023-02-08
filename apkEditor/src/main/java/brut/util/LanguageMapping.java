package brut.util;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

public class LanguageMapping {

    static LinkedHashMap<String, String> code2Name = new LinkedHashMap<String, String>();

    static {
        code2Name.put("-aa", "Afar");
        code2Name.put("-ab", "Abkhazian");
        code2Name.put("-af", "Afrikaans");
        code2Name.put("-ak", "Akan");
        code2Name.put("-sq", "Albanian");
        code2Name.put("-am", "Amharic");
        code2Name.put("-ar", "Arabic");
        code2Name.put("-an", "Aragonese");
        code2Name.put("-hy", "Armenian");
        code2Name.put("-as", "Assamese");
        code2Name.put("-av", "Avaric");
        code2Name.put("-ae", "Avestan");
        code2Name.put("-ay", "Aymara");
        code2Name.put("-az", "Azerbaijani");
        code2Name.put("-ba", "Bashkir");
        code2Name.put("-bm", "Bambara");
        code2Name.put("-eu", "Basque");
        code2Name.put("-be", "Belarusian");
        code2Name.put("-bn", "Bengali");
        code2Name.put("-bh", "Bihari languages+B372");
        code2Name.put("-bi", "Bislama");
        code2Name.put("-bo", "Tibetan");
        code2Name.put("-bs", "Bosnian");
        code2Name.put("-br", "Breton");
        code2Name.put("-bg", "Bulgarian");
        code2Name.put("-my", "Burmese");
        code2Name.put("-ca", "Catalan; Valencian");
        code2Name.put("-cs", "Czech");
        code2Name.put("-ch", "Chamorro");
        code2Name.put("-ce", "Chechen");
        code2Name.put("-zh", "Chinese");
        code2Name
                .put("cu",
                        "Church Slavic; Old Slavonic; Church Slavonic; Old Bulgarian; Old Church Slavonic");
        code2Name.put("-cv", "Chuvash");
        code2Name.put("-kw", "Cornish");
        code2Name.put("-co", "Corsican");
        code2Name.put("-cr", "Cree");
        code2Name.put("-cy", "Welsh");
        code2Name.put("-cs", "Czech");
        code2Name.put("-da", "Danish");
        code2Name.put("-de", "German");
        code2Name.put("-dv", "Divehi; Dhivehi; Maldivian");
        code2Name.put("-nl", "Dutch; Flemish");
        code2Name.put("-dz", "Dzongkha");
        code2Name.put("-el", "Greek, Modern (1453-)");
        code2Name.put("-en", "English");
        code2Name.put("-eo", "Esperanto");
        code2Name.put("-et", "Estonian");
        code2Name.put("-eu", "Basque");
        code2Name.put("-ee", "Ewe");
        code2Name.put("-fo", "Faroese");
        code2Name.put("-fa", "Persian");
        code2Name.put("-fj", "Fijian");
        code2Name.put("-fi", "Finnish");
        code2Name.put("-fr", "French");
        code2Name.put("-fy", "Western Frisian");
        code2Name.put("-ff", "Fulah");
        code2Name.put("-ka", "Georgian");
        code2Name.put("-de", "German");
        code2Name.put("-gd", "Gaelic; Scottish Gaelic");
        code2Name.put("-ga", "Irish");
        code2Name.put("-gl", "Galician");
        code2Name.put("-gv", "Manx");
        code2Name.put("-el", "Greek, Modern");
        code2Name.put("-gn", "Guarani");
        code2Name.put("-gu", "Gujarati");
        code2Name.put("-ht", "Haitian; Haitian Creole");
        code2Name.put("-ha", "Hausa");
        code2Name.put("-iw", "Hebrew");
        code2Name.put("-he", "Hebrew");
        code2Name.put("-hz", "Herero");
        code2Name.put("-hi", "Hindi");
        code2Name.put("-ho", "Hiri Motu");
        code2Name.put("-hr", "Croatian");
        code2Name.put("-hu", "Hungarian");
        code2Name.put("-hy", "Armenian");
        code2Name.put("-ig", "Igbo");
        code2Name.put("-is", "Icelandic");
        code2Name.put("-io", "Ido");
        code2Name.put("-ii", "Sichuan Yi; Nuosu");
        code2Name.put("-iu", "Inuktitut");
        code2Name.put("-ie", "Interlingue; Occidental");
        code2Name.put("-ia",
                "Interlingua (International Auxiliary Language Association)");
        code2Name.put("-in", "Indonesian");
        code2Name.put("-id", "Indonesian");
        code2Name.put("-ik", "Inupiaq");
        code2Name.put("-is", "Icelandic");
        code2Name.put("-it", "Italian");
        code2Name.put("-jv", "Javanese");
        code2Name.put("-ja", "Japanese");
        code2Name.put("-kl", "Kalaallisut; Greenlandic");
        code2Name.put("-kn", "Kannada");
        code2Name.put("-ks", "Kashmiri");
        code2Name.put("-ka", "Georgian");
        code2Name.put("-kr", "Kanuri");
        code2Name.put("-kk", "Kazakh");
        code2Name.put("-km", "Central Khmer");
        code2Name.put("-ki", "Kikuyu; Gikuyu");
        code2Name.put("-rw", "Kinyarwanda");
        code2Name.put("-ky", "Kirghiz; Kyrgyz");
        code2Name.put("-kv", "Komi");
        code2Name.put("-kg", "Kongo");
        code2Name.put("-ko", "Korean");
        code2Name.put("-kj", "Kuanyama; Kwanyama");
        code2Name.put("-ku", "Kurdish");
        code2Name.put("-lo", "Lao");
        code2Name.put("-la", "Latin");
        code2Name.put("-lv", "Latvian");
        code2Name.put("-li", "Limburgan; Limburger; Limburgish");
        code2Name.put("-ln", "Lingala");
        code2Name.put("-lt", "Lithuanian");
        code2Name.put("-lb", "Luxembourgish; Letzeburgesch");
        code2Name.put("-lu", "Luba-Katanga");
        code2Name.put("-lg", "Ganda");
        code2Name.put("-mk", "Macedonian");
        code2Name.put("-mh", "Marshallese");
        code2Name.put("-ml", "Malayalam");
        code2Name.put("-mi", "Maori");
        code2Name.put("-mr", "Marathi");
        code2Name.put("-ms", "Malay");
        code2Name.put("-mk", "Macedonian");
        code2Name.put("-mg", "Malagasy");
        code2Name.put("-mt", "Maltese");
        code2Name.put("-mn", "Mongolian");
        code2Name.put("-mi", "Maori");
        code2Name.put("-ms", "Malay");
        code2Name.put("-my", "Burmese");
        code2Name.put("-na", "Nauru");
        code2Name.put("-nv", "Navajo; Navaho");
        code2Name.put("-nr", "Ndebele, South; South Ndebele");
        code2Name.put("-nd", "Ndebele, North; North Ndebele");
        code2Name.put("-ng", "Ndonga");
        code2Name.put("-ne", "Nepali");
        code2Name.put("-nl", "Dutch; Flemish");
        code2Name.put("-nn", "Norwegian Nynorsk; Nynorsk, Norwegian");
        code2Name.put("-nb", "Bokmål, Norwegian; Norwegian Bokmål");
        code2Name.put("-no", "Norwegian");
        code2Name.put("-ny", "Chichewa; Chewa; Nyanja");
        code2Name.put("-oc", "Occitan (post 1500)");
        code2Name.put("-oj", "Ojibwa");
        code2Name.put("-or", "Oriya");
        code2Name.put("-om", "Oromo");
        code2Name.put("-os", "Ossetian; Ossetic");
        code2Name.put("-pa", "Panjabi; Punjabi");
        code2Name.put("-fa", "Persian");
        code2Name.put("-pi", "Pali");
        code2Name.put("-pl", "Polish");
        code2Name.put("-pt", "Portuguese");
        code2Name.put("-ps", "Pushto; Pashto");
        code2Name.put("-qu", "Quechua");
        code2Name.put("-rm", "Romansh");
        code2Name.put("-ro", "Romanian; Moldavian; Moldovan");
        code2Name.put("-ro", "Romanian; Moldavian; Moldovan");
        code2Name.put("-rn", "Rundi");
        code2Name.put("-ru", "Russian");
        code2Name.put("-sg", "Sango");
        code2Name.put("-sa", "Sanskrit");
        code2Name.put("-si", "Sinhala; Sinhalese");
        code2Name.put("-sk", "Slovak");
        code2Name.put("-sk", "Slovak");
        code2Name.put("-sl", "Slovenian");
        code2Name.put("-se", "Northern Sami");
        code2Name.put("-sm", "Samoan");
        code2Name.put("-sn", "Shona");
        code2Name.put("-sd", "Sindhi");
        code2Name.put("-so", "Somali");
        code2Name.put("-st", "Sotho, Southern");
        code2Name.put("-es", "Spanish; Castilian");
        code2Name.put("-sq", "Albanian");
        code2Name.put("-sc", "Sardinian");
        code2Name.put("-sr", "Serbian");
        code2Name.put("-ss", "Swati");
        code2Name.put("-su", "Sundanese");
        code2Name.put("-sw", "Swahili");
        code2Name.put("-sv", "Swedish");
        code2Name.put("-ty", "Tahitian");
        code2Name.put("-ta", "Tamil");
        code2Name.put("-tt", "Tatar");
        code2Name.put("-te", "Telugu");
        code2Name.put("-tg", "Tajik");
        code2Name.put("-tl", "Tagalog");
        code2Name.put("-th", "Thai");
        code2Name.put("-bo", "Tibetan");
        code2Name.put("-ti", "Tigrinya");
        code2Name.put("-to", "Tonga (Tonga Islands)");
        code2Name.put("-tn", "Tswana");
        code2Name.put("-ts", "Tsonga");
        code2Name.put("-tk", "Turkmen");
        code2Name.put("-tr", "Turkish");
        code2Name.put("-tw", "Twi");
        code2Name.put("-ug", "Uighur; Uyghur");
        code2Name.put("-uk", "Ukrainian");
        code2Name.put("-ur", "Urdu");
        code2Name.put("-uz", "Uzbek");
        code2Name.put("-ve", "Venda");
        code2Name.put("-vi", "Vietnamese");
        code2Name.put("-vo", "Volapük");
        code2Name.put("-cy", "Welsh");
        code2Name.put("-wa", "Walloon");
        code2Name.put("-wo", "Wolof");
        code2Name.put("-xh", "Xhosa");
        code2Name.put("-ji", "Yiddish");
        code2Name.put("-yi", "Yiddish");
        code2Name.put("-yo", "Yoruba");
        code2Name.put("-za", "Zhuang; Chuang");
        code2Name.put("-zh", "Chinese");
        code2Name.put("-zu", "Zulu");
    }

    public static String getLanguage(String code) {
        if (code.equals("")) {
            return "[ Default ]";
        }

        String shortCode = code;
        int pos = code.indexOf('-', 1);
        if (pos != -1) {
            shortCode = code.substring(0, pos);
        }

        String lang = code2Name.get(shortCode);
        if (lang != null) {
            return lang + " (" + code + ")";
        } else {
            return " (" + code + ")";
        }
    }

    public static int getSize() {
        return code2Name.size();
    }


    public static void getLanguages(String[] codes, String[] languages) {
        Set<Entry<String, String>> values = code2Name.entrySet();
        int index = 0;
        for (Entry<String, String> v : values) {
            codes[index] = v.getKey();
            languages[index] = v.getValue();
            index += 1;
        }
    }
}

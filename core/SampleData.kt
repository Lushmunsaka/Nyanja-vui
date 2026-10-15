package com.nynaja.vui

/** Full set of 20 verified health topics with English and Nyanja keywords and test sentences. */
object SampleData {

    fun topics(): List<Topic> = listOf(
        topic("pregnancy", "Pregnancy", "pregnancy", "Go to the clinic as soon as you know you are pregnant. Eat well and rest often.", "Mimba", "mimba", "Pitani ku kliniki mukangodziwa kuti muli ndi pakati. Idyani bwino ndipo mupume kawirikawiri."),
        topic("clinic", "Clinic", "clinic", "Visit the clinic at least eight times during your pregnancy.", "Kliniki", "kliniki", "Pitani ku kliniki nthawi zosachepera eyiti pa nthawi ya mimba."),
        topic("food", "Food", "food", "Eat beans, vegetables, eggs, fish and fruit every day. Drink enough clean water.", "Chakudya", "chakudya", "Idyani nyemba, ndiwo zamasamba, mazira, nsomba ndi zipatso tsiku lililonse. Imwani madzi aukhondo okwanira."),
        topic("danger", "Danger", "danger", "Go to the clinic at once if you have bleeding, a severe headache or blurred vision. Do not wait.", "Ngozi", "ngozi", "Pitani ku kliniki nthawi yomweyo ngati mukutuluka magazi, mukumva mutu waukulu kapena maso akuphimba. Musadikire."),
        topic("bleeding", "Bleeding", "bleeding", "Heavy bleeding in pregnancy or after birth is an emergency. Go to the clinic immediately.", "Magazi", "magazi", "Kutuluka magazi kwambiri pa nthawi ya mimba kapena pambuyo pobereka ndi ngozi. Pitani ku kliniki mwamsanga."),
        topic("labour", "Labour", "labour", "When labour pains start, go to the clinic. Give birth with a trained health worker.", "Kubereka", "kubereka", "Zowawa zobereka zikayamba, pitani ku kliniki. Berekani ndi katswiri wa zaumoyo."),
        topic("baby", "Baby", "baby", "Keep your newborn warm and hold the baby skin to skin. Keep the cord clean and dry.", "Khanda", "khanda", "Sungani khanda lanu lofunda ndi kuligwira pa chifuwa panu. Sungani mchombo wa khanda woyera ndi wouma."),
        topic("breastfeeding", "Breastfeeding", "breastfeeding", "Give only breast milk for the first six months. Start breastfeeding within one hour of birth.", "Kuyamwitsa", "kuyamwitsa", "Patsani khanda mkaka wa m'mawere wokha kwa miyezi isanu ndi umodzi yoyamba. Yambani kuyamwitsa pasanathe ola limodzi mutabereka."),
        topic("vaccine", "Vaccine", "vaccine", "Vaccines protect your baby from serious diseases. Take your baby for every vaccine on the health card.", "Katemera", "katemera", "Katemera amateteza khanda lanu ku matenda oopsa. Bweretsani khanda ku kliniki kukalandira katemera aliyense wolembedwa pa khadi la umoyo."),
        topic("contraception", "Contraception", "contraception", "You can choose a family planning method at the clinic. A health worker will help you decide.", "Kulera", "kulera", "Mungasankhe njira yolerera ku kliniki. Wogwira ntchito zaumoyo adzakuthandizani kusankha."),
        topic("malaria", "Malaria", "malaria", "Sleep under a treated mosquito net every night. Go to the clinic if you have a fever.", "Malungo", "malungo", "Gonani mu ukonde wa udzudzo wothira mankhwala tsiku lililonse usiku. Pitani ku kliniki ngati muli ndi malungo."),
        topic("hygiene", "Hygiene", "hygiene", "Wash your hands with soap before eating and after using the toilet.", "Ukhondo", "ukhondo", "Sambani m'manja ndi sopo musanadye komanso mutapita kuchimbudzi."),
        topic("water", "Water", "water", "Boil or treat your drinking water. Keep water containers covered.", "Madzi", "madzi", "Wiritsani kapena thirani mankhwala madzi akumwa. Phimbani zotengera madzi."),
        topic("rest", "Rest", "rest", "Rest when you are tired. Sleep on your side.", "Kupuma", "kupuma", "Mupume mukatopa. Gonani cham'mbali."),
        topic("medicine", "Medicine", "medicine", "Take only medicine given by the clinic. Ask a health worker before taking any other medicine.", "Mankhwala", "mankhwala", "Imwani mankhwala operekedwa ku kliniki okha. Funsani wogwira ntchito zaumoyo musanamwe mankhwala ena."),
        topic("pills", "Pills", "pills", "Take your iron and folic acid tablets every day, as the clinic advised.", "Mapiritsi", "mapiritsi", "Imwani mapiritsi a ayironi ndi folic acid tsiku lililonse monga mwauzidwa ku kliniki."),
        topic("postnatal", "Postnatal", "postnatal", "Visit the clinic soon after birth and again six weeks later. Tell the health worker if you have a fever or heavy bleeding.", "Pambuyo", "pambuyo", "Pitani ku kliniki mukangobereka kenako pakatha milungu isanu ndi umodzi. Uzani wogwira ntchito zaumoyo ngati muli ndi malungo kapena mukutuluka magazi kwambiri."),
        topic("child", "Child", "child", "Take your child to the clinic if they have a fever, fast breathing or diarrhoea. Give extra fluids when a child has diarrhoea.", "Mwana", "mwana", "Bweretsani mwana ku kliniki ngati ali ndi malungo, akupuma mofulumira, kapena akutsegula m'mimba. Mpatseni zakumwa zambiri akamatsegula m'mimba."),
        topic("help", "Help", "help", "If you need help, go to the nearest clinic or ask a community health worker.", "Thandizo", "thandizo", "Mukafuna thandizo, pitani ku kliniki yapafupi kapena funsani wogwira ntchito zaumoyo wa kumudzi."),
        topic("fever", "Fever", "fever", "A fever in pregnancy can be dangerous. Go to the clinic the same day.", "Kutentha", "kutentha", "Malungo pa nthawi ya mimba angakhale oopsa. Pitani ku kliniki tsiku lomwelo."),
    )

    private fun topic(
        id: String,
        enTitle: String,
        enKeyword: String,
        enContent: String,
        nyTitle: String,
        nyKeyword: String,
        nyContent: String,
    ) = Topic(
        id = id,
        titles = mapOf(Language.ENGLISH to enTitle, Language.NYANJA to nyTitle),
        content = mapOf(Language.ENGLISH to enContent, Language.NYANJA to nyContent),
        keywords = mapOf(
            Language.ENGLISH to setOf(enKeyword),
            Language.NYANJA to setOf(nyKeyword),
        ),
    )
}

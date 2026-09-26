package com.vitalis.data

import com.vitalis.core.model.FoodItem
import com.vitalis.core.model.FoodSource

/**
 * Curated Indonesian foods (spec §4.2.5 layer 3). Values are per 100 g of the dish as
 * served, typical home/warung recipes.
 *
 * ponytail: ~70 staples, approximations cross-checked only for Atwater consistency
 * (see LocalFoodSeedTest). `isVerified = false` until each row is checked against TKPI;
 * the spec target is ~500 items. Ids are stable — seeding is insert-if-absent, so
 * appending rows here reaches existing installs without a migration.
 */
object LocalFoodSeed {

    private fun f(slug: String, name: String, serving: String, servingG: Float, kcal: Float, p: Float, c: Float, fat: Float) = FoodItem(
        id = "local-$slug",
        name = name,
        source = FoodSource.LOCAL_ID,
        servingSizeG = servingG,
        servingLabel = serving,
        kcalPer100g = kcal,
        proteinPer100g = p,
        carbsPer100g = c,
        fatPer100g = fat,
        isVerified = false,
    )

    val items: List<FoodItem> = listOf(
        // Nasi & karbohidrat
        f("nasi-putih", "Nasi putih", "1 piring", 200f, 130f, 2.7f, 28.2f, 0.3f),
        f("nasi-merah", "Nasi merah", "1 piring", 200f, 110f, 2.6f, 23f, 0.9f),
        f("nasi-uduk", "Nasi uduk", "1 porsi", 200f, 180f, 3f, 30f, 5.5f),
        f("nasi-kuning", "Nasi kuning", "1 porsi", 200f, 185f, 3.2f, 31f, 5.2f),
        f("nasi-goreng", "Nasi goreng", "1 piring", 250f, 168f, 6.3f, 21f, 6.5f),
        f("mie-goreng", "Mie goreng", "1 piring", 250f, 190f, 5.5f, 26f, 7.2f),
        f("mi-instan-goreng", "Mi instan goreng", "1 bungkus", 85f, 465f, 9f, 62f, 20f),
        f("bubur-ayam", "Bubur ayam", "1 mangkuk", 350f, 90f, 4f, 12f, 2.8f),
        f("lontong-sayur", "Lontong sayur", "1 porsi", 350f, 95f, 3f, 13f, 3.5f),
        f("roti-tawar", "Roti tawar", "1 lembar", 30f, 265f, 9f, 49f, 3.2f),
        f("oatmeal", "Oatmeal (dimasak dengan air)", "1 mangkuk", 250f, 71f, 2.5f, 12f, 1.5f),
        f("kentang-rebus", "Kentang rebus", "1 buah", 150f, 87f, 1.9f, 20f, 0.1f),
        f("ubi-rebus", "Ubi rebus", "1 buah", 150f, 86f, 1.6f, 20f, 0.1f),
        f("singkong-rebus", "Singkong rebus", "1 potong", 100f, 160f, 1.4f, 38f, 0.3f),
        f("jagung-rebus", "Jagung rebus", "1 tongkol", 150f, 96f, 3.4f, 19f, 1.5f),

        // Lauk hewani
        f("ayam-bakar-paha", "Ayam bakar (paha)", "1 potong", 100f, 230f, 24f, 4f, 13f),
        f("ayam-goreng-paha", "Ayam goreng (paha)", "1 potong", 100f, 260f, 25f, 3f, 16.5f),
        f("ayam-geprek", "Ayam geprek", "1 porsi", 150f, 270f, 20f, 10f, 17f),
        f("dada-ayam-panggang", "Dada ayam panggang", "100 g", 100f, 165f, 31f, 0f, 3.6f),
        f("opor-ayam", "Opor ayam", "1 potong", 150f, 165f, 14f, 3f, 11f),
        f("sate-ayam", "Sate ayam + bumbu kacang", "10 tusuk", 150f, 225f, 18f, 10f, 12.5f),
        f("rendang-sapi", "Rendang sapi", "1 potong", 70f, 195f, 19.5f, 4.5f, 11f),
        f("semur-daging", "Semur daging sapi", "1 potong", 70f, 160f, 17f, 6f, 7.5f),
        f("rawon", "Rawon", "1 mangkuk", 350f, 90f, 7f, 3.5f, 5.5f),
        f("gulai-kambing", "Gulai kambing", "1 porsi", 150f, 170f, 13f, 4f, 11.5f),
        f("sate-kambing", "Sate kambing", "10 tusuk", 150f, 240f, 20f, 6f, 15f),
        f("ikan-goreng", "Ikan nila goreng", "1 ekor", 120f, 195f, 23f, 3f, 10.5f),
        f("ikan-bakar", "Ikan bakar", "1 ekor", 150f, 150f, 25f, 2f, 4.8f),
        f("pepes-ikan", "Pepes ikan", "1 bungkus", 100f, 120f, 18f, 3f, 4f),
        f("telur-rebus", "Telur rebus", "1 butir", 55f, 155f, 12.6f, 1.1f, 10.6f),
        f("telur-dadar", "Telur dadar", "1 porsi", 60f, 195f, 13f, 1.5f, 15f),
        f("telur-balado", "Telur balado", "1 butir", 70f, 190f, 11f, 5f, 14f),

        // Lauk nabati
        f("tempe-goreng", "Tempe goreng", "1 potong", 25f, 225f, 14f, 9f, 15f),
        f("tempe-mendoan", "Tempe mendoan", "1 potong", 40f, 245f, 10f, 18f, 15f),
        f("tahu-goreng", "Tahu goreng", "1 potong", 40f, 175f, 11f, 5f, 12.5f),

        // Sayur
        f("sayur-asem", "Sayur asem", "1 mangkuk", 200f, 30f, 0.8f, 6f, 0.3f),
        f("sayur-sop", "Sayur sop", "1 mangkuk", 250f, 35f, 1.5f, 5.5f, 0.8f),
        f("capcay", "Capcay", "1 porsi", 200f, 70f, 3f, 6f, 4f),
        f("tumis-kangkung", "Tumis kangkung", "1 porsi", 100f, 65f, 2.5f, 4f, 4.5f),
        f("gado-gado", "Gado-gado", "1 porsi", 250f, 130f, 6f, 9f, 8f),
        f("pecel", "Pecel", "1 porsi", 200f, 120f, 5.5f, 11f, 6.5f),
        f("lalapan-sambal", "Lalapan & sambal", "1 porsi", 100f, 60f, 1.5f, 6f, 3.5f),
        f("sambal", "Sambal", "1 sdm", 15f, 90f, 1.5f, 8f, 6f),

        // Jajanan & street food
        f("bakso", "Bakso kuah", "1 mangkuk", 350f, 70f, 4.5f, 6.5f, 2.9f),
        f("soto-ayam", "Soto ayam", "1 mangkuk", 400f, 78f, 6f, 6f, 3.4f),
        f("mie-ayam", "Mie ayam", "1 mangkuk", 300f, 130f, 7f, 18f, 3.5f),
        f("ketoprak", "Ketoprak", "1 porsi", 300f, 150f, 6f, 18f, 6f),
        f("siomay", "Siomay", "1 porsi", 250f, 150f, 8f, 18f, 5f),
        f("batagor", "Batagor", "1 porsi", 200f, 230f, 9f, 22f, 12f),
        f("pempek", "Pempek + cuko", "1 porsi", 150f, 200f, 8f, 30f, 5f),
        f("martabak-telur", "Martabak telur", "1 potong", 60f, 250f, 10f, 20f, 14.5f),
        f("martabak-manis", "Martabak manis", "1 potong", 75f, 330f, 6.5f, 45f, 14f),
        f("pisang-goreng", "Pisang goreng", "1 buah", 60f, 240f, 2f, 35f, 10.5f),
        f("bakwan", "Bakwan sayur", "1 buah", 40f, 280f, 5f, 28f, 16.5f),
        f("kerupuk", "Kerupuk", "1 keping", 10f, 480f, 1f, 70f, 22f),
        f("kacang-goreng", "Kacang tanah goreng", "1 genggam", 30f, 590f, 25f, 18f, 48f),

        // Buah
        f("pisang-ambon", "Pisang ambon", "1 buah sedang", 118f, 89f, 1.1f, 22.8f, 0.3f),
        f("pepaya", "Pepaya", "1 potong", 150f, 43f, 0.5f, 10.8f, 0.3f),
        f("apel", "Apel", "1 buah", 180f, 52f, 0.3f, 13.8f, 0.2f),
        f("jeruk", "Jeruk", "1 buah", 130f, 47f, 0.9f, 11.8f, 0.1f),
        f("mangga", "Mangga", "1 buah", 200f, 60f, 0.8f, 15f, 0.4f),
        f("semangka", "Semangka", "1 potong", 200f, 30f, 0.6f, 7f, 0.2f),
        f("alpukat", "Alpukat", "1/2 buah", 100f, 160f, 2f, 8.5f, 14.7f),

        // Minuman & susu
        f("susu-full-cream", "Susu sapi full cream", "1 gelas", 250f, 61f, 3.2f, 4.8f, 3.3f),
        f("yogurt-plain", "Yogurt plain", "1 cup", 150f, 61f, 3.5f, 4.7f, 3.3f),
        f("kopi-susu-gula-aren", "Kopi susu gula aren", "1 gelas", 250f, 58f, 1.2f, 9f, 1.8f),
        f("kopi-hitam", "Kopi hitam tanpa gula", "1 cangkir", 200f, 2f, 0.1f, 0f, 0f),
        f("teh-manis", "Teh manis", "1 gelas", 250f, 32f, 0f, 8f, 0f),
        f("jus-alpukat", "Jus alpukat", "1 gelas", 300f, 110f, 1.5f, 12f, 6.5f),
        f("air-kelapa", "Air kelapa", "1 gelas", 250f, 19f, 0.7f, 3.7f, 0.2f),
    )
}

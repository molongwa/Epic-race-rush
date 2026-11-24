package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R

enum class RaceMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val laps: Int
) {
    CITY_SPRINT(
        id = "city_sprint",
        title = "City Sprint",
        subtitle = "Battle 5 rival supercars through live urban traffic",
        badge = "3 LAPS • AI RIVALS",
        laps = 3
    ),
    TIME_ATTACK(
        id = "time_attack",
        title = "Time Attack",
        subtitle = "Solo hot-lap challenge for Bronze, Silver & Gold medals",
        badge = "2 LAPS • MEDAL RUN",
        laps = 2
    ),
    ENDLESS_HIGHWAY(
        id = "endless_highway",
        title = "Endless Highway",
        subtitle = "Weave city traffic endlessly & chain near-miss combos",
        badge = "SURVIVAL • COMBO x5",
        laps = 99
    )
}

enum class SteeringMode(val label: String, val description: String) {
    TOUCH_BUTTONS("Touch Pedals", "Responsive left/right steer pads & Nitro button"),
    DRAG_STEER("Slide Steer", "Smooth finger slide anywhere on the lower road"),
    TILT_SENSOR("Tilt Wheel", "Use device accelerometer to steer like a wheel")
}

enum class UpgradePart(val label: String, val statLabel: String, val baseCost: Int) {
    ENGINE("Engine Block", "Top Speed", 350),
    TURBO("Twin Turbo", "Acceleration", 320),
    HANDLING("Sport Suspension", "Corner Grip", 300),
    NITRO("Nitrous Oxide", "Boost Power", 280)
}

data class PaintSwatch(
    val name: String,
    val argbLong: Long
) {
    val color: Color get() = Color(argbLong)
}

data class CarModel(
    val id: String,
    val name: String,
    val classLabel: String,
    val engineSpec: String,
    val priceCredits: Int,
    val defaultPaintHex: Long,
    val baseTopSpeedKmh: Int,
    val baseZeroToHundredSec: Float,
    val baseHandling: Float, // 0.65f..0.98f
    val baseNitroCapacity: Float, // 0.70f..1.0f
    val hasGtWing: Boolean,
    val bodyStyle: CarBodyStyle,
    @DrawableRes val showcaseImageRes: Int
)

enum class CarBodyStyle {
    GT_COUPE,
    MID_ENGINE_EXOTIC,
    ELECTRIC_HYPER,
    V12_SUPERCAR
}

data class DriverProfile(
    val id: String,
    val name: String,
    val callsign: String,
    val cityOrigin: String,
    val perkTitle: String,
    val perkDescription: String,
    val driftChargeMultiplier: Float,
    val topSpeedBonusPct: Float,
    val bio: String,
    @DrawableRes val portraitRes: Int
)

data class CityTrack(
    val id: String,
    val name: String,
    val district: String,
    val description: String,
    val lapLengthMeters: Float,
    val skyTopColor: Long,
    val skyBottomColor: Long,
    val buildingAccentColor: Long,
    val roadTintColor: Long,
    val goldTimeMs: Long,
    val silverTimeMs: Long,
    val trafficDensity: Float,
    val curveIntensity: Float
)

object RacingCatalog {
    val paintSwatches = listOf(
        PaintSwatch("Crimson", 0xFFD91E36),
        PaintSwatch("Pearl Silver", 0xFFCBD5E1),
        PaintSwatch("Cobalt Blue", 0xFF2563EB),
        PaintSwatch("Obsidian", 0xFF1F2937),
        PaintSwatch("British Emerald", 0xFF059669),
        PaintSwatch("Solar Amber", 0xFFD97706)
    )

    val cars = listOf(
        CarModel(
            id = "apex_v8_gt",
            name = "Apex V8 GT",
            classLabel = "GT STREET",
            engineSpec = "4.0L Twin-Turbo V8 • 610 HP",
            priceCredits = 0,
            defaultPaintHex = 0xFFD91E36,
            baseTopSpeedKmh = 296,
            baseZeroToHundredSec = 3.3f,
            baseHandling = 0.78f,
            baseNitroCapacity = 0.78f,
            hasGtWing = false,
            bodyStyle = CarBodyStyle.GT_COUPE,
            showcaseImageRes = R.drawable.img_car_gt_coupe
        ),
        CarModel(
            id = "veloce_rs",
            name = "Veloce RS Coupé",
            classLabel = "SPORT PRO",
            engineSpec = "3.8L Flat-6 Bi-Turbo • 670 HP",
            priceCredits = 2200,
            defaultPaintHex = 0xFFCBD5E1,
            baseTopSpeedKmh = 318,
            baseZeroToHundredSec = 2.9f,
            baseHandling = 0.86f,
            baseNitroCapacity = 0.84f,
            hasGtWing = true,
            bodyStyle = CarBodyStyle.MID_ENGINE_EXOTIC,
            showcaseImageRes = R.drawable.img_car_gt_coupe
        ),
        CarModel(
            id = "phantom_e_gt",
            name = "Phantom Apex-E",
            classLabel = "HYPER AWD",
            engineSpec = "Tri-Motor Torque Vectoring • 920 HP",
            priceCredits = 5500,
            defaultPaintHex = 0xFF2563EB,
            baseTopSpeedKmh = 338,
            baseZeroToHundredSec = 2.3f,
            baseHandling = 0.90f,
            baseNitroCapacity = 0.92f,
            hasGtWing = true,
            bodyStyle = CarBodyStyle.ELECTRIC_HYPER,
            showcaseImageRes = R.drawable.img_car_gt_coupe
        ),
        CarModel(
            id = "titan_obsidian",
            name = "Titan V12 Strada",
            classLabel = "APEX CLASS",
            engineSpec = "6.5L Naturally Aspirated V12 • 830 HP",
            priceCredits = 10500,
            defaultPaintHex = 0xFF1F2937,
            baseTopSpeedKmh = 362,
            baseZeroToHundredSec = 2.5f,
            baseHandling = 0.94f,
            baseNitroCapacity = 1.00f,
            hasGtWing = true,
            bodyStyle = CarBodyStyle.V12_SUPERCAR,
            showcaseImageRes = R.drawable.img_car_gt_coupe
        )
    )

    val drivers = listOf(
        DriverProfile(
            id = "marcus",
            name = "Marcus Vance",
            callsign = "NIGHTHAWK",
            cityOrigin = "Chicago Metro",
            perkTitle = "+15% Drift Nitro",
            perkDescription = "Charges Nitro tank 15% faster during high-speed drifts and near-misses.",
            driftChargeMultiplier = 1.15f,
            topSpeedBonusPct = 0.0f,
            bio = "Former underground touring champion known for fearless late-braking overtakes through dense downtown boulevards.",
            portraitRes = R.drawable.img_driver_marcus
        ),
        DriverProfile(
            id = "elena",
            name = "Elena Rostova",
            callsign = "VALKYRIE",
            cityOrigin = "Monaco / Milan",
            perkTitle = "+5% Top Speed & Grip",
            perkDescription = "Smooth telemetry lines grant +5% higher top velocity and tighter cornering stability.",
            driftChargeMultiplier = 1.0f,
            topSpeedBonusPct = 0.05f,
            bio = "Precision GT3 endurance specialist bringing pro-circuit apex discipline to metropolitan expressways.",
            portraitRes = R.drawable.img_driver_elena
        )
    )

    val tracks = listOf(
        CityTrack(
            id = "downtown_neon",
            name = "Downtown Neon Loop",
            district = "Financial Core • Dusk",
            description = "Wet glass-tower avenues with glowing neon storefronts and sweeping 4-lane S-bends.",
            lapLengthMeters = 1800f,
            skyTopColor = 0xFF090D1D,
            skyBottomColor = 0xFF1E2B4E,
            buildingAccentColor = 0xFF38BDF8,
            roadTintColor = 0xFF1E2533,
            goldTimeMs = 46_000L,
            silverTimeMs = 54_000L,
            trafficDensity = 0.72f,
            curveIntensity = 0.85f
        ),
        CityTrack(
            id = "harbor_express",
            name = "Harbor Expressway",
            district = "Bay Bridge • Golden Hour",
            description = "Elevated coastal urban highway bathed in amber sunset light with fast drafting straights.",
            lapLengthMeters = 2100f,
            skyTopColor = 0xFF1F1635,
            skyBottomColor = 0xFF9A3412,
            buildingAccentColor = 0xFFFBBF24,
            roadTintColor = 0xFF232732,
            goldTimeMs = 52_000L,
            silverTimeMs = 61_000L,
            trafficDensity = 0.85f,
            curveIntensity = 1.0f
        ),
        CityTrack(
            id = "skyline_overpass",
            name = "Skyline Overpass",
            district = "Midtown Viaduct • Midnight",
            description = "High-altitude city viaducts surrounded by illuminated skyscrapers and heavy midnight traffic.",
            lapLengthMeters = 2400f,
            skyTopColor = 0xFF040711,
            skyBottomColor = 0xFF0F2942,
            buildingAccentColor = 0xFFE11D48,
            roadTintColor = 0xFF171D28,
            goldTimeMs = 58_000L,
            silverTimeMs = 68_000L,
            trafficDensity = 1.0f,
            curveIntensity = 1.15f
        )
    )

    fun carById(id: String): CarModel = cars.firstOrNull { it.id == id } ?: cars.first()
    fun driverById(id: String): DriverProfile = drivers.firstOrNull { it.id == id } ?: drivers.first()
    fun trackById(id: String): CityTrack = tracks.firstOrNull { it.id == id } ?: tracks.first()
}

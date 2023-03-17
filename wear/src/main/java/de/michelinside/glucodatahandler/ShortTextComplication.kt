package de.michelinside.glucodatahandler

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.*
import de.michelinside.glucodatahandler.common.BatteryReceiver
import de.michelinside.glucodatahandler.common.ReceiveData
import de.michelinside.glucodatahandler.common.Utils
import de.michelinside.glucodatahandler.common.WearPhoneConnection

open class ShortClucoseComplication:  BgValueComplicationService() {
}

class ShortGlucoseWithIconComplication: BgValueComplicationService() {
    override fun getIcon(): MonochromaticImage = glucoseIcon()
}

class ShortGlucoseWithTrendComplication: BgValueComplicationService() {
    override fun getIcon(): MonochromaticImage = arrowIcon()
}

class ShortGlucoseWithDeltaComplication: ShortClucoseComplication() {
    override fun getTitle(): PlainComplicationText = deltaText()
}

class ShortGlucoseWithTrendTextComplication: ShortClucoseComplication() {
    override fun getTitle(): PlainComplicationText = trendText()
}

class ShortGlucoseWithDeltaAndTrendComplication: ShortClucoseComplication() {
    override fun getTitle(): PlainComplicationText = deltaText()
    override fun getIcon(): MonochromaticImage = arrowIcon()
    override fun getLongTextComplicationData(): ComplicationData {
        return LongTextComplicationData.Builder(
            plainText(" Δ: " + ReceiveData.getDeltaAsString() + "   " + ReceiveData.getRateSymbol().toString() + " (" + ReceiveData.getRateAsString() + ")"),
            descriptionText()
        )
            .setSmallImage(glucoseImage())
            .setTapAction(getTapAction())
            .build()
    }
}

class LongGlucoseWithDeltaAndTrendAndTimeComplication: ShortClucoseComplication() {
    override fun getLongTextComplicationData(): ComplicationData {
        return LongTextComplicationData.Builder(
            plainText(" Δ: " + ReceiveData.getDeltaAsString()),
            descriptionText()
        )
            .setTitle(timeText())
            .setSmallImage(getGlucoseTrendImage())
            .setTapAction(getTapAction())
            .build()
    }
}

open class ShortGlucoseWithTrendRangeComplication: BgValueComplicationService() {
    override fun getRangeValueComplicationData(): ComplicationData {
        return RangedValueComplicationData.Builder(
            value = Utils.rangeValue( ReceiveData.rate, -3.5F, +3.5F),
            min = 0F,
            max = 4F,
            contentDescription = descriptionText()
        )
            .setTitle(getTitle())
            .setText(getText())
            .setMonochromaticImage(getIcon())
            .setTapAction(getTapAction())
            .build()
    }
}

class ShortGlucoseWithDeltaAndTrendRangeComplication: ShortGlucoseWithTrendRangeComplication() {
    override fun getTitle(): PlainComplicationText = deltaText()
}

class ShortDeltaWithTrendArrowAndTrendRangeComplication: ShortGlucoseWithTrendRangeComplication() {
    override fun getText(): PlainComplicationText = deltaText()
    override fun getIcon(): MonochromaticImage = arrowIcon()
}

open class ShortDeltaComplication: ShortClucoseComplication() {
    override fun getText(): PlainComplicationText = deltaText()
}

class ShortDeltaWithTrendComplication: ShortDeltaComplication() {
    override fun getIcon(): MonochromaticImage = arrowIcon()
}

class ShortDeltaWithIconComplication: ShortDeltaComplication() {
    override fun getIcon(): MonochromaticImage = deltaIcon()
}

open class ShortTrendComplication: ShortClucoseComplication() {
    override fun getText(): PlainComplicationText = trendText()
}

class ShortTrendWithTrendArrowComplication: ShortTrendComplication() {
    override fun getIcon(): MonochromaticImage = arrowIcon()
}

class ShortTrendWithIconComplication: ShortTrendComplication() {
    override fun getIcon(): MonochromaticImage = trendIcon()
}

class BatteryLevelComplication: BgValueComplicationService() {
    override fun getTapAction(): PendingIntent? {
        val launchIntent = Intent(this, WaerActivity::class.java)
        launchIntent.action = Intent.ACTION_MAIN
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        return PendingIntent.getActivity(
            applicationContext,
            3,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
    override fun getTitle(): PlainComplicationText? {
        val levels = WearPhoneConnection.getBatterLevels()
        if (levels.isNotEmpty() && levels[0] > 0) {
            return plainText("\uD83D\uDCF1" + levels[0].toString() + "%")
        }
        return null
    }
    override fun getText(): PlainComplicationText =
        plainText("⌚" + BatteryReceiver.batteryPercentage.toString() + "%")
}


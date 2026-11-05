package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.model.CurrencyCatalog
import com.example.model.ExpenseItem
import com.example.model.PaymentEntry
import com.example.ui.theme.SplitBleePalettes
import com.example.ui.theme.SplitBleeThemePalette
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    private const val CHANNEL_ID = "splitblee_realtime_channel"

    fun sendRealTimeNotification(context: Context, title: String, message: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "SplitBlee Real-Time Ledger",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Real-time notifications for splits, ledger settlements, and 4K/8K exports"
                }
                nm.createNotificationChannel(channel)
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            nm.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        } catch (_: Exception) {
            // Ignore in headless environments
        }
    }

    private fun composeColorToAndroidColor(c: androidx.compose.ui.graphics.Color): Int {
        return Color.argb(
            (c.alpha * 255).toInt().coerceIn(0, 255),
            (c.red * 255).toInt().coerceIn(0, 255),
            (c.green * 255).toInt().coerceIn(0, 255),
            (c.blue * 255).toInt().coerceIn(0, 255)
        )
    }

    fun buildTxtReport(
        scopeLabel: String,
        expenses: List<ExpenseItem>,
        payments: List<PaymentEntry>,
        themeName: String = "Ocean Blue",
        resolutionTag: String = "Standard"
    ): String {
        val sb = StringBuilder()
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        sb.appendLine("==================================================")
        sb.appendLine("SPLITBLEE REAL-TIME FINANCIAL REPORT ($scopeLabel)")
        sb.appendLine("Theme Palette: $themeName | Export Mode: $resolutionTag")
        sb.appendLine("Generated Live: $nowStr")
        sb.appendLine("Total Expenses: ${expenses.size} | Recorded Payments: ${payments.size}")
        sb.appendLine("==================================================")
        sb.appendLine()
        sb.appendLine("--- EXPENSES ---")
        if (expenses.isEmpty()) {
            sb.appendLine("No expenses in selected scope.")
        } else {
            expenses.forEachIndexed { idx, exp ->
                val totalFormatted = CurrencyCatalog.formatMinor(exp.totalMinor, exp.currency)
                val subtotalFormatted = CurrencyCatalog.formatMinor(exp.subtotalMinor, exp.currency)
                val taxFormatted = CurrencyCatalog.formatMinor(exp.taxMinor, exp.currency)
                val discountFormatted = CurrencyCatalog.formatMinor(exp.discountMinor, exp.currency)
                sb.appendLine("${idx + 1}. ${exp.title} [${exp.status}]")
                sb.appendLine("   Date: ${exp.expenseDate} | Place: ${exp.place}")
                sb.appendLine("   Currency: ${exp.currency} | Subtotal: $subtotalFormatted | Tax (${exp.taxPercent}%): $taxFormatted | Discount: -$discountFormatted")
                sb.appendLine("   Final Total: $totalFormatted | Split: ${exp.splitMethod} (${exp.participantCount} people)")
                sb.appendLine("   Participants: ${exp.participantNames.joinToString(", ")}")
                sb.appendLine()
            }
        }
        sb.appendLine("--- IMMUTABLE PAYMENT LEDGER ---")
        if (payments.isEmpty()) {
            sb.appendLine("No recorded ledger payments in selected scope.")
        } else {
            payments.forEachIndexed { idx, pay ->
                val amt = CurrencyCatalog.formatMinor(pay.amountMinor, pay.currency)
                sb.appendLine("${idx + 1}. [${pay.entryType}] ${pay.payerName} -> ${pay.payeeName}: $amt (${pay.currency}) via ${pay.method} for '${pay.expenseTitle}'")
            }
        }
        return sb.toString()
    }

    fun buildCsvReport(expenses: List<ExpenseItem>): String {
        val sb = StringBuilder()
        sb.appendLine("\"ExpenseID\",\"Date\",\"Month\",\"Title\",\"Category\",\"Place\",\"Group\",\"Participants\",\"SplitMethod\",\"Currency\",\"SubtotalMinor\",\"TaxMinor\",\"DiscountMinor\",\"TotalMinor\",\"FormattedTotal\",\"PaidMinor\",\"Status\"")
        expenses.forEach { exp ->
            val formattedTotal = CurrencyCatalog.formatMinor(exp.totalMinor, exp.currency)
            sb.appendLine(
                listOf(
                    exp.id,
                    exp.expenseDate,
                    exp.monthKey,
                    exp.title,
                    exp.category,
                    exp.place,
                    exp.groupName ?: "",
                    exp.participantNames.joinToString("; "),
                    exp.splitMethod,
                    exp.currency,
                    exp.subtotalMinor.toString(),
                    exp.taxMinor.toString(),
                    exp.discountMinor.toString(),
                    exp.totalMinor.toString(),
                    formattedTotal,
                    exp.paidMinor.toString(),
                    exp.status
                ).joinToString(",") { field ->
                    "\"${field.replace("\"", "\"\"")}\""
                }
            )
        }
        return sb.toString()
    }

    /**
     * Generates a vector-crisp PDF report in Standard (A4), 4K UHD (3840x2160), or 8K UHD (7680x4320) canvas resolution,
     * styled with the user's active Pastel / Ocean theme.
     */
    fun buildPdfSummaryBytes(
        scopeLabel: String,
        expenses: List<ExpenseItem>,
        payments: List<PaymentEntry>,
        themeName: String = "Ocean Blue",
        resolution: String = "4K UHD"
    ): ByteArray {
        val palette: SplitBleeThemePalette = SplitBleePalettes.forName(themeName)
        val (pageW, pageH, scale) = when {
            resolution.contains("8K", ignoreCase = true) -> Triple(4320, 7680, 4.8f)
            resolution.contains("4K", ignoreCase = true) -> Triple(2160, 3840, 2.4f)
            else -> Triple(900, 1600, 1.0f)
        }

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
        val page = doc.startPage(pageInfo)
        drawThemedReportOnCanvas(
            canvas = page.canvas,
            width = pageW.toFloat(),
            height = pageH.toFloat(),
            scale = scale,
            palette = palette,
            scopeLabel = scopeLabel,
            resolutionLabel = resolution,
            expenses = expenses,
            payments = payments
        )
        doc.finishPage(page)

        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    /**
     * Generates a high-resolution 4K UHD or 8K UHD PNG image poster of the financial report / split receipt
     * styled with the user's active Pastel / Ocean theme.
     */
    fun buildHighResPosterPngBytes(
        scopeLabel: String,
        expenses: List<ExpenseItem>,
        payments: List<PaymentEntry>,
        themeName: String = "Ocean Blue",
        resolution: String = "4K UHD"
    ): ByteArray {
        val palette = SplitBleePalettes.forName(themeName)
        val runtime = Runtime.getRuntime()
        val freeBytes = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())

        // Choose highest safe dimensions for 4K (2160x3840) or 8K (4320x7680) within available JVM heap
        val (targetW, targetH, scale) = when {
            resolution.contains("8K", ignoreCase = true) && freeBytes > 180L * 1024L * 1024L ->
                Triple(4320, 7680, 4.8f)
            resolution.contains("8K", ignoreCase = true) && freeBytes > 80L * 1024L * 1024L ->
                Triple(2880, 5120, 3.2f)
            freeBytes > 60L * 1024L * 1024L ->
                Triple(2160, 3840, 2.4f)
            else ->
                Triple(1080, 1920, 1.2f)
        }

        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawThemedReportOnCanvas(
            canvas = canvas,
            width = targetW.toFloat(),
            height = targetH.toFloat(),
            scale = scale,
            palette = palette,
            scopeLabel = scopeLabel,
            resolutionLabel = "$resolution (${targetW}x${targetH})",
            expenses = expenses,
            payments = payments
        )

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun drawThemedReportOnCanvas(
        canvas: Canvas,
        width: Float,
        height: Float,
        scale: Float,
        palette: SplitBleeThemePalette,
        scopeLabel: String,
        resolutionLabel: String,
        expenses: List<ExpenseItem>,
        payments: List<PaymentEntry>
    ) {
        val bgCol = composeColorToAndroidColor(palette.lightBackground)
        val primaryCol = composeColorToAndroidColor(palette.primary)
        val deepCol = composeColorToAndroidColor(palette.deep)
        val cardCol = composeColorToAndroidColor(palette.lightSurface)
        val containerCol = composeColorToAndroidColor(palette.lightContainer)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgCol }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        val pad = 48f * scale
        var y = pad

        // Header Banner with Pastel / Ocean Gradient
        val bannerHeight = 210f * scale
        val bannerRect = RectF(pad, y, width - pad, y + bannerHeight)
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                bannerRect.left,
                bannerRect.top,
                bannerRect.right,
                bannerRect.bottom,
                primaryCol,
                deepCol,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(bannerRect, 32f * scale, 32f * scale, bannerPaint)

        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f * scale
            isFakeBoldText = true
        }
        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 255, 255, 255)
            textSize = 19f * scale
        }

        val nowStr = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.US).format(Date())
        canvas.drawText(
            "SplitBlee — Real-Time Ledger Report",
            pad + 36f * scale,
            y + 72f * scale,
            headerTitlePaint
        )
        canvas.drawText(
            "Scope: $scopeLabel  •  Theme: ${palette.name}  •  Resolution: $resolutionLabel",
            pad + 36f * scale,
            y + 120f * scale,
            headerSubPaint
        )
        canvas.drawText(
            "Generated Live: $nowStr  •  ${expenses.size} Splits  •  ${payments.size} Ledger Entries",
            pad + 36f * scale,
            y + 162f * scale,
            headerSubPaint
        )

        y += bannerHeight + 36f * scale

        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = deepCol
            textSize = 24f * scale
            isFakeBoldText = true
        }
        val titleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = deepCol
            textSize = 20f * scale
            isFakeBoldText = true
        }
        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(86, 106, 142)
            textSize = 16f * scale
        }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardCol }
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = containerCol }

        canvas.drawText("Split Expenses (${expenses.size})", pad, y, sectionPaint)
        y += 22f * scale

        if (expenses.isEmpty()) {
            val emptyRect = RectF(pad, y, width - pad, y + 100f * scale)
            canvas.drawRoundRect(emptyRect, 20f * scale, 20f * scale, cardPaint)
            canvas.drawText("No active expenses in this scope.", pad + 28f * scale, y + 58f * scale, subTextPaint)
            y += 126f * scale
        } else {
            expenses.take(12).forEachIndexed { index, exp ->
                val rowH = 104f * scale
                if (y + rowH < height - 160f * scale) {
                    val rowRect = RectF(pad, y, width - pad, y + rowH)
                    canvas.drawRoundRect(rowRect, 22f * scale, 22f * scale, cardPaint)

                    val totalStr = CurrencyCatalog.formatMinor(exp.totalMinor, exp.currency)
                    canvas.drawText(
                        "${index + 1}. ${exp.title}  (${exp.currency} $totalStr)",
                        pad + 26f * scale,
                        y + 44f * scale,
                        titleTextPaint
                    )
                    canvas.drawText(
                        "${exp.expenseDate}  •  ${exp.place}  •  ${exp.splitMethod} (${exp.participantCount} people)  •  Status: ${exp.status}",
                        pad + 26f * scale,
                        y + 78f * scale,
                        subTextPaint
                    )
                    y += rowH + 16f * scale
                }
            }
        }

        y += 20f * scale
        if (y + 160f * scale < height) {
            canvas.drawText("Immutable Payment Ledger (${payments.size})", pad, y, sectionPaint)
            y += 20f * scale
            payments.take(6).forEachIndexed { idx, pay ->
                val rowH = 74f * scale
                if (y + rowH < height - 60f * scale) {
                    val rowRect = RectF(pad, y, width - pad, y + rowH)
                    canvas.drawRoundRect(rowRect, 16f * scale, 16f * scale, badgePaint)
                    val amt = CurrencyCatalog.formatMinor(pay.amountMinor, pay.currency)
                    canvas.drawText(
                        "${idx + 1}. [${pay.entryType}] ${pay.payerName} -> ${pay.payeeName}: $amt via ${pay.method} (${pay.expenseTitle})",
                        pad + 24f * scale,
                        y + 45f * scale,
                        titleTextPaint
                    )
                    y += rowH + 12f * scale
                }
            }
        }
    }

    fun writeBytesToUri(context: Context, uri: Uri, bytes: ByteArray): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: error("Unable to open selected document output stream")
    }

    fun shareTextContent(context: Context, title: String, content: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        val chooser = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

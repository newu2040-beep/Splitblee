package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.model.CurrencyCatalog
import com.example.model.ExpenseItem
import com.example.model.PaymentEntry
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    fun buildTxtReport(
        scopeLabel: String,
        expenses: List<ExpenseItem>,
        payments: List<PaymentEntry>
    ): String {
        val sb = StringBuilder()
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        sb.appendLine("==================================================")
        sb.appendLine("SPLITBLEE FINANCIAL REPORT ($scopeLabel)")
        sb.appendLine("Generated: $nowStr")
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
        sb.appendLine("--- PAYMENT LEDGER ---")
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

    fun buildPdfSummaryBytes(
        scopeLabel: String,
        expenses: List<ExpenseItem>,
        payments: List<PaymentEntry>
    ): ByteArray {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(16, 42, 99)
            textSize = 18f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 11f
        }

        var y = 48f
        canvas.drawText("SplitBlee — Expense & Settlement Report ($scopeLabel)", 36f, y, titlePaint)
        y += 24f
        canvas.drawText("Records: ${expenses.size} expenses, ${payments.size} ledger payments", 36f, y, bodyPaint)
        y += 24f

        expenses.take(22).forEachIndexed { index, exp ->
            val totalStr = CurrencyCatalog.formatMinor(exp.totalMinor, exp.currency)
            val line = "${index + 1}. ${exp.title} • ${exp.expenseDate} • ${exp.currency} $totalStr [${exp.status}]"
            canvas.drawText(line.take(90), 36f, y, bodyPaint)
            y += 18f
            val subLine = "   Place: ${exp.place} | Split: ${exp.splitMethod} (${exp.participantCount} people)"
            canvas.drawText(subLine.take(95), 36f, y, bodyPaint)
            y += 20f
        }

        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
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

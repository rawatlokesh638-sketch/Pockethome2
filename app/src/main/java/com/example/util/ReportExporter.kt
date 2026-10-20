package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.Transaction
import com.example.ui.DashboardViewModel
import java.io.File
import java.io.FileOutputStream

object ReportExporter {

    fun generateCsv(context: Context, transactions: List<Transaction>): File {
        val file = File(context.cacheDir, "report.csv")
        file.bufferedWriter().use { out ->
            out.write("Title,Amount,Date,Category,PaymentMethod\n")
            transactions.forEach {
                out.write("${it.title},${it.amount},${it.date},${it.category},${it.paymentMethod}\n")
            }
        }
        return file
    }

    fun generatePdf(context: Context, transactions: List<Transaction>): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 20f
        canvas.drawText("Financial Report", 50f, 50f, paint)
        
        paint.textSize = 12f
        var y = 100f
        transactions.forEach {
            canvas.drawText("${it.date}: ${it.title} - ${DashboardViewModel.formatCurrency(it.amount)}", 50f, y, paint)
            y += 20f
        }

        document.finishPage(page)
        val file = File(context.cacheDir, "report.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    fun getFileUri(context: Context, file: File) = 
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}

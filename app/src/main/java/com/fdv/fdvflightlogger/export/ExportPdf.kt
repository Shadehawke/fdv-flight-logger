package com.fdv.fdvflightlogger.export

import androidx.core.graphics.toColorInt
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

object ExportPdf {

    private const val COLOR_SHADOW_BLACK = "#0E0E10"
    private const val COLOR_IVORY_LIGHT = "#F4F1EA"

    data class HeaderInfo(
        val pilotId: String,
        val pilotName: String,
        val hub: String
    )

    // A4 page size (points-ish). Good default for a logbook export.
    private const val PAGE_W = 595
    private const val PAGE_H = 842

    private const val MARGIN = 32
    private const val HEADER_BAR_H = 56
    private const val HEADER_INFO_H = 44
    private const val ROW_H = 18

    private val dateFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())

    private data class Col(val title: String, val w: Int)

    // Notes column fills remaining width (w = 0)
    private val cols = listOf(
        Col("Date", 92),
        Col("Flight", 70),
        Col("A/C", 70),
        Col("DEP", 40),
        Col("ARR", 40),
        Col("Block", 52),
        Col("Air", 46),
        Col("Fuel", 50),
        Col("ZFW", 54),
        Col("Notes", 0)
    )

    fun buildPdf(
        flights: List<FlightLogEntity>,
        header: HeaderInfo
    ): PdfDocument {
        val doc = PdfDocument()

        val paintBar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SHADOW_BLACK.toColorInt()
        }

        val paintBarText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_IVORY_LIGHT.toColorInt()
            textSize = 14f
            isFakeBoldText = true
        }


        val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 11f
        }
        val paintHead = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            isFakeBoldText = true
        }
        val paintCell = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9f
        }
        val paintGrid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val usableW = PAGE_W - (MARGIN * 2)
        val fixedW = cols.filter { it.w > 0 }.sumOf { it.w }
        val notesW = max(90, usableW - fixedW)

        // Column left X positions
        val colXs = mutableListOf<Int>()
        var x = MARGIN
        for (c in cols) {
            colXs.add(x)
            x += if (c.w == 0) notesW else c.w
        }

        val tableTop = MARGIN + HEADER_BAR_H + HEADER_INFO_H + 8
        val tableBottom = PAGE_H - MARGIN

        // Rows per page: subtract 1 row for column header
        val rowsPerPage = ((tableBottom - tableTop) / ROW_H - 1).coerceAtLeast(1)

        val rows = if (flights.isEmpty()) listOf<FlightLogEntity?>(null) else flights

        var pageNum = 1
        var index = 0

        while (index < rows.size) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            drawHeader(canvas, header, pageNum, paintBar, paintBarText, paintSub)

            var y = tableTop
            drawColumnHeader(canvas, colXs, y, paintHead, paintGrid)
            y += ROW_H

            val end = minOf(rows.size, index + rowsPerPage)
            while (index < end) {
                val entity = rows[index]
                val values = if (entity == null) {
                    listOf("—", "—", "—", "—", "—", "—", "—", "—", "—", "No flights logged yet.")
                } else {
                    rowValues(entity)
                }

                drawRow(canvas, colXs, y, notesW, values, paintCell, paintGrid)
                y += ROW_H
                index++
            }

            doc.finishPage(page)
            pageNum++
        }

        return doc
    }

    private fun drawHeader(
        canvas: Canvas,
        header: HeaderInfo,
        pageNum: Int,
        paintBar: Paint,
        paintBarText: Paint,
        paintSub: Paint
    ) {
        // Header bar
        canvas.drawRect(
            0f, 0f,
            PAGE_W.toFloat(), HEADER_BAR_H.toFloat(),
            paintBar
        )
        canvas.drawText(
            "FDV Flight Logger — Logbook Export",
            MARGIN.toFloat(),
            36f,
            paintBarText
        )

        val line1 = "Pilot: ${header.pilotId} • ${header.pilotName}"
        val line2 = "Hub: ${header.hub}"
        canvas.drawText(line1, MARGIN.toFloat(), (HEADER_BAR_H + 22).toFloat(), paintSub)
        canvas.drawText(line2, MARGIN.toFloat(), (HEADER_BAR_H + 38).toFloat(), paintSub)

        val pageLabel = "Page $pageNum"
        val px = PAGE_W - MARGIN - paintSub.measureText(pageLabel)
        canvas.drawText(pageLabel, px, (HEADER_BAR_H + 22).toFloat(), paintSub)
    }

    private fun drawColumnHeader(
        canvas: Canvas,
        colXs: List<Int>,
        y: Int,
        paintHead: Paint,
        paintGrid: Paint
    ) {
        // Column titles
        for (i in cols.indices) {
            val title = cols[i].title
            canvas.drawText(title, colXs[i].toFloat() + 2f, (y + 12).toFloat(), paintHead)
        }

        // Header underline
        canvas.drawLine(
            MARGIN.toFloat(),
            (y + ROW_H).toFloat(),
            (PAGE_W - MARGIN).toFloat(),
            (y + ROW_H).toFloat(),
            paintGrid
        )

        // Vertical grid lines (full page table area)
        for (i in cols.indices) {
            val x = colXs[i].toFloat()
            canvas.drawLine(x, y.toFloat(), x, (PAGE_H - MARGIN).toFloat(), paintGrid)
        }
        // Right edge
        canvas.drawLine(
            (PAGE_W - MARGIN).toFloat(),
            y.toFloat(),
            (PAGE_W - MARGIN).toFloat(),
            (PAGE_H - MARGIN).toFloat(),
            paintGrid
        )
    }

    private fun drawRow(
        canvas: Canvas,
        colXs: List<Int>,
        y: Int,
        notesW: Int,
        values: List<String>,
        paintCell: Paint,
        paintGrid: Paint
    ) {
        val baseline = (y + 12).toFloat()

        for (i in cols.indices) {
            val x = colXs[i].toFloat() + 2f
            val maxW = columnWidth(i, notesW) - 6
            val text = ellipsize(values.getOrElse(i) { "" }, paintCell, maxW.toFloat())
            canvas.drawText(text, x, baseline, paintCell)
        }

        // Row line
        canvas.drawLine(
            MARGIN.toFloat(),
            (y + ROW_H).toFloat(),
            (PAGE_W - MARGIN).toFloat(),
            (y + ROW_H).toFloat(),
            paintGrid
        )
    }

    private fun columnWidth(index: Int, notesW: Int): Int {
        val c = cols[index]
        return if (c.w == 0) notesW else c.w
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ell = "…"
        var lo = 0
        var hi = text.length
        while (lo < hi) {
            val mid = (lo + hi) / 2
            val candidate = text.substring(0, mid) + ell
            if (paint.measureText(candidate) <= maxWidth) lo = mid + 1 else hi = mid
        }
        val cut = max(0, lo - 1)
        return text.substring(0, cut) + ell
    }

    private fun rowValues(f: FlightLogEntity): List<String> {
        val date = dateFmt.format(Instant.ofEpochMilli(f.createdAtEpochMs))

        val notes = when {
            f.scratchpad.isNotBlank() -> f.scratchpad.replace("\n", " ").trim()
            f.route.isNotBlank() -> "Route: ${f.route}".trim()
            else -> ""
        }

        return listOf(
            date,
            f.flightNumber,
            f.aircraft,
            f.dep,
            f.arr,
            f.blockTime,
            f.airTime,
            f.fuel,
            f.zfw,
            notes
        )
    }
}

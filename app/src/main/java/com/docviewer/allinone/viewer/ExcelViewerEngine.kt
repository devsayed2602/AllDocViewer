package com.docviewer.allinone.viewer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream

data class ExcelContent(
    val sheets: List<ExcelSheet>
)

data class ExcelSheet(
    val name: String,
    val headers: List<String>,
    val rows: List<List<String>>
)

class ExcelViewerEngine {

    private var content: ExcelContent? = null

    fun getContent(): ExcelContent = content ?: ExcelContent(emptyList())

    suspend fun loadFromFile(file: File) = withContext(Dispatchers.IO) {
        val inputStream = FileInputStream(file)
        val workbook: Workbook = if (file.name.endsWith(".xlsx", ignoreCase = true)) {
            XSSFWorkbook(inputStream)
        } else {
            try {
                HSSFWorkbook(inputStream)
            } catch (e: Exception) {
                inputStream.close()
                content = ExcelContent(
                    listOf(ExcelSheet("Error", listOf("Message"), listOf(listOf("Could not read this Excel file: ${e.message}"))))
                )
                return@withContext
            }
        }

        val sheets = mutableListOf<ExcelSheet>()
        for (i in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(i)
            val name = sheet.sheetName ?: "Sheet ${i + 1}"
            val rows = mutableListOf<List<String>>()
            var headers = listOf<String>()

            for (rowIndex in 0..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                val cells = mutableListOf<String>()
                for (cellIndex in 0 until row.lastCellNum) {
                    val cell = row.getCell(cellIndex)
                    val value = when (cell?.cellType) {
                        CellType.STRING -> cell.stringCellValue
                        CellType.NUMERIC -> {
                            val num = cell.numericCellValue
                            if (num == num.toLong().toDouble()) {
                                num.toLong().toString()
                            } else {
                                num.toString()
                            }
                        }
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        CellType.FORMULA -> try { cell.stringCellValue } catch (_: Exception) {
                            try { cell.numericCellValue.toString() } catch (_: Exception) { "" }
                        }
                        else -> ""
                    }
                    cells.add(value)
                }
                if (rowIndex == 0) {
                    headers = cells
                } else {
                    rows.add(cells)
                }
            }
            sheets.add(ExcelSheet(name, headers, rows))
        }

        workbook.close()
        inputStream.close()
        content = ExcelContent(sheets)
    }

    fun close() {
        content = null
    }
}

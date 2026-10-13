package com.vela.notes.data.scanner

import java.io.File

const val MAX_SCAN_PAGES = 50
const val SCAN_PAGE_LIMIT_MESSAGE = "Tối đa 50 trang."

data class ScanPage(
    val id: String,
    val imageFile: File,
    val thumbnailFile: File,
    val quarterTurns: Int = 0,
    val filter: ScanFilter = ScanFilter.ORIGINAL,
)

sealed interface ScanDraftResult {
    data class Ready(val pages: List<ScanPage>) : ScanDraftResult
    data object LimitReached : ScanDraftResult
    data object ConfirmDiscardLast : ScanDraftResult
}

fun addScanPage(pages: List<ScanPage>, page: ScanPage): ScanDraftResult {
    if (pages.size >= MAX_SCAN_PAGES) return ScanDraftResult.LimitReached
    if (pages.any { it.id == page.id }) return ScanDraftResult.Ready(pages)
    return ScanDraftResult.Ready(pages + page)
}

fun moveScanPage(pages: List<ScanPage>, fromIndex: Int, toIndex: Int): List<ScanPage> {
    if (fromIndex !in pages.indices || toIndex !in pages.indices || fromIndex == toIndex) return pages
    val updated = pages.toMutableList()
    val page = updated.removeAt(fromIndex)
    updated.add(toIndex, page)
    return updated
}

fun updateScanPage(
    pages: List<ScanPage>,
    id: String,
    transform: (ScanPage) -> ScanPage,
): List<ScanPage> = pages.map { page -> if (page.id == id) transform(page) else page }

fun deleteScanPage(
    pages: List<ScanPage>,
    id: String,
    discardLast: Boolean = false,
): ScanDraftResult {
    val page = pages.firstOrNull { it.id == id } ?: return ScanDraftResult.Ready(pages)
    if (pages.size == 1 && !discardLast) return ScanDraftResult.ConfirmDiscardLast
    deletePageFiles(page)
    return ScanDraftResult.Ready(pages.filterNot { it.id == id })
}

fun discardScanPages(pages: List<ScanPage>) {
    pages.forEach(::deletePageFiles)
}

private fun deletePageFiles(page: ScanPage) {
    page.imageFile.delete()
    if (page.thumbnailFile != page.imageFile) page.thumbnailFile.delete()
}

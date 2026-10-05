package com.filmdozu

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Document

class FilmDozuProvider : MainAPI() {
    override var mainUrl = "https://filmdozu.com"
    override var name = "FilmDozu"
    override val hasMainPage = true
    override var lang = "tr"
    override val supportedTypes = setOf(TvType.Movie)

    override suspend fun getMainPage(page: Int, request: ExtraRequest): HomePageResponse? {
        val document = app.get(mainUrl).document
        val homeItems = ArrayList<HomePageList>()

        val movies = document.select("div.film-box, article.item, div.movie-poster").mapNotNull { element ->
            val title = element.selectFirst("h2, h3, .title, .film-name")?.text() ?: return@mapNotNull null
            val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val posterUrl = element.selectFirst("img")?.attr("src") ?: element.selectFirst("img")?.attr("data-src")

            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = fixUrlNull(posterUrl)
            }
        }

        if (movies.isNotEmpty()) {
            homeItems.add(HomePageList("Son Eklenen Filmler", movies))
        }

        return newHomePageResponse(homeItems)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document

        return document.select("div.film-box, article.item, div.movie-poster").mapNotNull { element ->
            val title = element.selectFirst("h2, h3, .title, .film-name")?.text() ?: return@mapNotNull null
            val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val posterUrl = element.selectFirst("img")?.attr("src") ?: element.selectFirst("img")?.attr("data-src")

            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = fixUrlNull(posterUrl)
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1, .entry-title")?.text() ?: return null
        val poster = document.selectFirst(".poster img, .film-poster img")?.attr("src")
        val description = document.selectFirst(".description, .overview, .film-content")?.text()
        val year = document.selectFirst(".year, .release-date")?.text()?.toIntOrNull()

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = fixUrlNull(poster)
            this.plot = description
            this.year = year
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val iframes = document.select("iframe[src]").map { it.attr("src") }

        for (iframeUrl in iframes) {
            val fixedUrl = fixUrl(iframeUrl)
            loadExtractor(fixedUrl, data, subtitleCallback, callback)
        }

        return true
    }
}

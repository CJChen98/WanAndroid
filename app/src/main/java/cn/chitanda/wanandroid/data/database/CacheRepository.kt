package cn.chitanda.wanandroid.data.database

import android.content.Context
import cn.chitanda.wanandroid.data.bean.Article

/**
 * @Author:       Chen
 * @Date:         2021/3/12 14:39
 * @Description:
 */
class CacheRepository private constructor(context: Context) {
    private val db by lazy { CacheDataBase.getInstance(context) }

    private val articleDao by lazy { db.articleDao() }

    companion object {
        @Volatile
        private var instance: CacheRepository? = null

        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: CacheRepository(context).also { instance = it }
        }
    }

    fun getCachedArticles() = articleDao.getArticles()

    fun cacheArticles(articles: List<Article.Data>) = articleDao.insertArticles(articles)

    fun clearArticleCache() = articleDao.deleteAllArticles()
}
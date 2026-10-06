package com.unus.one.repository;

public interface ArticleReposiotry {
  /**
   * 讀取文章資料。
   *
   * @throws Exception 文章讀取失敗時
   */
  public void readArticle() throws Exception;
}

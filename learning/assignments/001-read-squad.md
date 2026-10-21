# 作業 001 讀取 SQuAD JSON 與建立文章型別

本次將知識庫的文章與段落從 JSON 讀成 Java 物件，讓後續檢索與匯入功能有可使用的資料。你會從現有的空方法開始，自己完成實作與測試；這份題目提供契約和提示。

狀態：待你實作  
前置條件：Java 25 JDK、專案 Gradle Wrapper，以及現有 SQuAD JSON  
返回：[作業路線](../README.md)

## 情境與目標

RAG 需要先取得知識內容，才能搜尋片段並整理答案。SQuAD 將文章放在 `data` 陣列，每篇文章有 `title` 與 `paragraphs`，段落正文是 `context`。

本次只取得文章標題與段落正文。段落內的 `qas` 是問題及參考答案，後續用來評估，這次不把它們當成知識正文，也不建立對應型別。

完成後，呼叫 `ArticleRepositoryImpl.readArticle()` 可取得 `List<Article>`，能遍歷文章及段落，不需要呼叫端再解析 `JsonNode`。

## 學習重點

| Java 或 Spring 概念 | 簡短 C# 對照 | 本次用途 |
| --- | --- | --- |
| `record` | C# record；Java accessor 如 `title()` | 表達文章與段落資料 |
| `List<Article>` | `List<Article>` 或 `IReadOnlyList<Article>` 的具型別集合概念 | 限定回傳文章型別；Java List 不保證唯讀 |
| `Path`、`Files.readString` | 路徑處理與 `File.ReadAllText` | 以 UTF-8 讀取檔案 |
| Jackson `ObjectMapper`、`JsonNode` | JSON 解析器與 `JsonDocument` 的概念對照 | 檢查 JSON 結構並轉成模型 |
| 建構式注入、`@Repository` | 建構式 DI 與容器註冊 | 由 Spring 提供路徑設定與解析器 |
| `throws IOException` | C# 沒有 checked exception 宣告 | 明確傳遞資料讀取失敗 |

專案使用 `tools.jackson.databind` 的 Jackson 3；沿用現有 import。`JsonNode` 是 JSON 樹節點，不是 Java `Map`，也不是最後回傳的文章物件。

## 任務範圍

### 輸入與型別契約

預設來源是 [`data/source/squad-dev-v2.0.json`](../../data/source/squad-dev-v2.0.json)。從 `resume/one` 執行時，相對路徑以該目錄為準；使用既有的 `demo.data.json-path` 設定，不在方法內寫死個人電腦的絕對路徑。

在 `com.flow.one.model` 建立以下兩個 record：

| 型別 | 欄位 | 意義 |
| --- | --- | --- |
| `Article` | `String title`、`List<Paragraph> paragraphs` | 一篇文章及其段落 |
| `Paragraph` | `String context` | 一個完整段落正文 |

將現有 `ArticleReposiotry` 與其實作的契約同步調整成 `List<Article> readArticle() throws IOException`。沿用目前拼字 `ArticleReposiotry`，這次不另外做介面改名。

`ArticleRepositoryImpl` 使用 `@Repository`，公開建構式依序接收 `String jsonPath` 與 `ObjectMapper objectMapper`。前者透過 `@Value` 讀取 `demo.data.json-path`，預設值沿用 `data/source/squad-dev-v2.0.json`；保存為正規化的絕對 `Path`。測試可直接傳入暫存檔路徑與解析器，不必啟動整個 Spring 應用。

### 行為契約

- 回傳所有文章與所有段落，保留來源陣列順序。
- 保留 `title` 與 `context` 原文，不 trim、不截短、不翻譯。
- 合法的 `data: []` 回傳空 List；合法的 `paragraphs: []` 保留該文章並提供空段落 List。
- `data` 必須是陣列；每篇文章的 `title` 必須是字串、`paragraphs` 必須是陣列；每個段落的 `context` 必須是字串。缺少欄位或型別不符時回報 `IOException`，不靜默略過或以空字串代替。
- 檔案無法讀取時傳遞 `IOException`。JSON 語法錯誤時轉成 `IOException`，保留解析例外作為 cause；結構錯誤訊息包含欄位位置，例如 `data[0].paragraphs`。
- 忽略本次不使用的 `version`、`qas` 與其他額外欄位。文字值為空字串仍是合法字串；本題不增加內容品質規則。

不新增 HTTP endpoint、Service、SQLite 寫入、JPA entity、搜尋、模型呼叫或併發。保留既有 benchmark 的程式與量測方式。

### 會修改的位置

- [`ArticleReposiotry.java`](../../src/main/java/com/unus/one/repository/ArticleReposiotry.java)：回傳與例外契約。
- [`ArticleRepositoryImpl.java`](../../src/main/java/com/unus/one/repository/impl/ArticleRepositoryImpl.java)：路徑、注入、讀取、結構檢查與模型轉換。
- `src/main/java/com/unus/one/model/`：新增 `Article`、`Paragraph`。
- `src/test/java/com/unus/one/repository/impl/ArticleRepositoryImplTests.java`：新增作業測試。

新建構式、record 與方法使用繁體中文 Javadoc，依參數、回傳值、例外補上 `@param`、`@return`、`@throws`。既有註解保留，契約改變的部分同步更新。

## 步驟提示

### 第一步 先讀懂資料

先只看一篇文章與一個段落，不必閱讀整份大檔。用下面的形狀辨識每一層；這是人造範例，不是資料集節錄：

```json
{
  "version": "v2.0",
  "data": [
    {
      "title": "Example article",
      "paragraphs": [
        { "context": "Example paragraph.", "qas": [] }
      ]
    }
  ]
}
```

先回答：`data` 與 `paragraphs` 各是哪種 JSON 型別？`title` 與 `context` 在哪一層？一篇文章能有多少個段落？

### 第二步 定義模型與方法

依型別表建立兩個 record，再同步修改 interface 與 implementation 的方法宣告。讓方法能回傳文章集合，而不是只印在 console。辨識 `Article` 是文章模型，`JsonNode` 是暫時用於解析的節點。

### 第三步 讀取檔案與解析 JSON

參考 [`SearchBenchmarkServiceImpl`](../../src/main/java/com/unus/one/service/impl/SearchBenchmarkServiceImpl.java) 的建構式與 `searchJson()`，理解 `@Value`、`Path.of`、`Files.readString`、`readTree` 的順序。

只參考讀取與走訪方式。該 benchmark 會搜尋、截短預覽並略過部分不完整結構；本題要保留完整文章及嚴格檢查必要欄位，不照搬那些行為。

`Files.readString` 使用 UTF-8 並自行管理檔案讀取資源。沿用注入的 mapper，不在每篇文章內建立解析器；若改用自行開啟的 reader，必須以 try-with-resources 管理。

### 第四步 走訪與轉換

先檢查根節點的 `data`，再逐篇取得 `title` 與 `paragraphs`，接著逐段取得 `context`。以 `isArray()`、`isString()` 檢查型別；通過檢查後才取字串值及建立 record。

每篇文章要建立自己的段落集合，不能把前一篇段落留在下一篇。先用一般 `for` 迴圈完成，不要求 Stream 或抽取通用 helper。

### 第五步 完成例外與測試

本專案 Jackson 3 的 `tools.jackson.core.JacksonException` 是 RuntimeException，不是 IOException。只在 JSON 解析邊界捕捉它，包成保留 cause 的 IOException；不用 `catch (Exception)` 把所有程式錯誤混在一起。

測試沿用 JUnit Jupiter，用 `@TempDir` 建立 UTF-8 小型 JSON fixture，直接建構 repository。測試讀取成功、順序與失敗行為，不修改官方 JSON，也不靠啟動服務後目視 console 才驗證。

## 驗收條件

### 先用小型 fixture 驗證

將這份人造資料寫入測試暫存檔：

```json
{
  "version": "v2.0",
  "data": [
    {
      "title": "Alpha",
      "paragraphs": [
        { "context": "First paragraph.", "qas": [] },
        { "context": "  第二段保留空白。  ", "qas": [] }
      ]
    },
    {
      "title": "Beta",
      "paragraphs": [
        { "context": "Third paragraph.", "qas": [] }
      ]
    }
  ]
}
```

| 案例 | 輸入 | 預期結果 |
| --- | --- | --- |
| 正常讀取與順序 | 上述 fixture | 2 篇文章；依序為 Alpha、Beta，段落數為 2、1；正文與來源逐字一致，包含中文及首尾空白 |
| 空文章集合 | `{"data":[]}` | 空 List，不是 null |
| 空段落集合 | `{"data":[{"title":"Empty","paragraphs":[]}]}` | 保留 1 篇文章，其段落集合為空 |
| 檔案不存在 | 暫存目錄中未建立的檔案路徑 | IOException，不能假裝讀取成功 |
| JSON 語法錯誤 | 只有 `{` 的檔案 | IOException，cause 保留解析錯誤 |
| 必要欄位錯誤 | 分別測試 data 缺少或不是陣列、title 缺少或非字串、paragraphs 缺少或非陣列、context 缺少或非字串 | IOException；訊息指向錯誤欄位，不回傳不完整集合 |

必要欄位案例可逐項測試或用參數化測試。此題驗收錯誤類型與欄位位置，不要求完整例外訊息逐字相同。

### 再對照專案資料集

使用預設 SQuAD JSON，檢查：

- 文章總數 **35**，所有文章段落數合計 **1,204**。
- 第一篇文章標題 **Normans**，段落數 **39**。
- 第一個段落正文與來源 `data[0].paragraphs[0].context` 完整一致。
- 方法回傳具型別集合；呼叫端能取得 `title()`、`paragraphs()` 與 `context()`。

這些數字對應本專案的 dev split；計數必須由回傳物件算出，不能寫死在讀取方法。

在 PowerShell 從 `C:\Users\zanew\Documents\GitHub\unus\resume\one` 執行：

```powershell
.\gradlew.bat test --tests "com.flow.one.repository.impl.ArticleRepositoryImplTests" --console=plain
```

本題通過後，執行既有測試確認整合未退步：

```powershell
.\gradlew.bat test --console=plain
```

以上是你實作後要執行的命令。交付前，勾選以下完成條件：

- [ ] 新型別與 interface／implementation 契約一致，編譯成功。
- [ ] 小型 fixture 的成功、順序、UTF-8、邊界及失敗案例通過。
- [ ] 真實資料集的文章、段落與首篇內容核對通過。
- [ ] 原有測試通過，benchmark 行為未改動。
- [ ] 提供下列方法說明，能解釋實作而不只展示輸出。

## 方法說明

交付時附上修改檔案、實際測試命令及結果，並用自己的話回答：

1. JSON 檔案如何經過 Path、檔案文字、JsonNode，最後變成 `List<Article>`？
2. 為什麼文章含段落集合？為什麼不直接回傳 JsonNode 或 Map？
3. `@Value` 提供的是什麼？Spring 注入與測試直接建構有什麼差別？
4. 為什麼讀取資料放在 Repository？這次為什麼不需要 Controller？
5. 檔案不存在、JSON 語法錯誤與欄位型別錯誤各在哪一步發現？
6. 誰負責關閉檔案資源？為什麼不吞掉例外回傳空集合？
7. 測試怎麼證明段落沒有跨文章混在一起，以及沒有被截短或 trim？

交付訊息可用這個格式：

```text
作業：001
修改檔案：
執行命令與結果：
真實資料核對：文章數、段落數、第一篇標題與段落數
資料流與設計理由：
卡住或尚未完成的部分：
請幫我依驗收條件 review。
```

Review 會先讀你的實際程式，再核對測試與原始 JSON。具體問題以檔案位置及最小修正提示回覆；你完成且能解釋方法後，才安排下一份檢索作業。

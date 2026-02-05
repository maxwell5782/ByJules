# Game Design Document: Star Guardian (星際守護者)

## 1. 遊戲概述 (Game Overview)
*   **遊戲名稱**：星際守護者 (Star Guardian)
*   **遊戲類型**：放置型養成遊戲 (Idle / Nurturing Game)
*   **遊戲平台**：Web (HTML5 / JavaScript)
*   **核心目標**：玩家扮演宇宙中的守護者，透過收集「星塵」(Stardust) 來培育一顆初生的恆星，使其從微弱的火花成長為璀璨的星辰。

## 2. 核心機制 (Core Mechanics)

### 2.1 資源收集 (Resource Collection)
*   **手動收集**：點擊螢幕中央的「星核」(Star Heart) 可獲得星塵。
*   **自動收集**：購買並升級「收集器」(Collectors) 後，系統會隨時間自動產生星塵 (Stardust Per Second, SPS)。

### 2.2 養成系統 (Nurturing System)
*   **星核升級**：消耗星塵提升星核等級。等級提升後，恆星的外觀會發生變化（體積變大、光芒增強、顏色改變）。
*   **等級階段**：
    1.  1-10 級：微弱火花 (Dim Spark)
    2.  11-50 級：發光球體 (Glowing Orb)
    3.  51-100 級：璀璨恆星 (Radiant Star)
    4.  101+ 級：星系中心 (Galactic Center)

### 2.3 升級系統 (Upgrade System)
*   **點擊強化**：提升每次點擊獲得的星塵量。
*   **收集器**：
    *   **星塵塵埃 (Stardust Mote)**：每秒產生 1 星塵。
    *   **彗星尾跡 (Comet Tail)**：每秒產生 5 星塵。
    *   **星雲吸塵器 (Nebula Vacuum)**：每秒產生 20 星塵。

## 3. 遊戲流程 (Game Loop)
1.  **收集**：點擊或等待自動產生星塵。
2.  **消耗**：投入星塵用於升級點擊力或購買收集器。
3.  **成長**：觀察星核等級提升與外觀變化。
4.  **擴張**：解鎖更高級的收集設施，達成更高的 SPS。

## 4. 使用者介面 (UI Design)
*   **頂部區域**：顯示當前擁有的星塵總數與每秒產量 (SPS)。
*   **中央區域**：顯示正在成長的恆星（點擊目標）。
*   **底部區域**：升級選單分頁（點擊強化、自動收集器、成就）。

## 5. 技術堆疊 (Technical Stack)
*   **前端**：HTML5, CSS3, Vanilla JavaScript.
*   **狀態管理**：簡單的 JavaScript 物件，儲存於 LocalStorage 以便存檔。

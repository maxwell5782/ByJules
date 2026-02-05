const { gameState, handleStarClick, calculateSPS, buyCollector, upgradeClick, levelUp, calculateLevelUpCost } = require('./game.js');

function assert(condition, message) {
    if (!condition) {
        console.error('測試失敗:', message);
        process.exit(1);
    }
    console.log('測試通過:', message);
}

// 重置狀態
function resetState() {
    gameState.stardust = 0;
    gameState.starLevel = 1;
    gameState.clickPower = 1;
    for (const key in gameState.collectors) {
        gameState.collectors[key].count = 0;
        gameState.collectors[key].cost = gameState.collectors[key].baseCost;
    }
}

resetState();

// 測試 1: 點擊
handleStarClick();
assert(gameState.stardust === 1, '點擊應增加 1 星塵');

// 測試 2: 升級
const levelCost = calculateLevelUpCost();
assert(levelCost === 100, '初始升級成本應為 100');
gameState.stardust = 100;
const leveled = levelUp();
assert(leveled === true, '擁有 100 星塵時應可升級');
assert(gameState.starLevel === 2, '星核等級應為 2');
assert(gameState.stardust === 0, '升級後星塵應為 0');

// 測試 3: 其他收集器
gameState.stardust = 100;
const boughtComet = buyCollector('comets');
assert(boughtComet === true, '擁有 100 星塵時應可購買彗星');
assert(gameState.collectors.comets.count === 1, '彗星數量應為 1');
calculateSPS();
assert(gameState.sps === 5, '購買一個彗星後 SPS 應為 5');

console.log('所有邏輯測試皆已通過！');

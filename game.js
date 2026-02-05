// 星際守護者 - 核心遊戲邏輯

const gameState = {
    stardust: 0,
    sps: 0,
    clickPower: 1,
    starLevel: 1,
    collectors: {
        motes: {
            count: 0,
            baseCost: 15,
            cost: 15,
            production: 1
        },
        comets: {
            count: 0,
            baseCost: 100,
            cost: 100,
            production: 5
        },
        nebulas: {
            count: 0,
            baseCost: 500,
            cost: 500,
            production: 20
        }
    },
    upgrades: {
        click: {
            level: 1,
            cost: 10
        }
    }
};

// DOM 元素獲取
const getEl = (id) => typeof document !== 'undefined' ? document.getElementById(id) : null;

const stardustDisplay = getEl('stardust-count');
const spsDisplay = getEl('sps-count');
const starHeart = getEl('star-heart');
const starLevelDisplay = getEl('star-level-display');
const levelUpBtn = getEl('level-up-btn');

// 更新介面
function updateUI() {
    if (stardustDisplay) stardustDisplay.innerText = Math.floor(gameState.stardust);
    if (spsDisplay) spsDisplay.innerText = gameState.sps.toFixed(1);

    const levelCost = calculateLevelUpCost();
    if (starLevelDisplay) {
        let stageName = "微弱火花";
        if (gameState.starLevel > 50) stageName = "星系中心";
        else if (gameState.starLevel > 20) stageName = "璀璨恆星";
        else if (gameState.starLevel > 5) stageName = "發光球體";
        starLevelDisplay.innerText = `等級: ${gameState.starLevel} (${stageName})`;
    }

    if (levelUpBtn) {
        levelUpBtn.innerText = `星核升級 (成本: ${Math.ceil(levelCost)})`;
        levelUpBtn.disabled = gameState.stardust < levelCost;
    }

    // 更新收集器資訊
    for (const key in gameState.collectors) {
        const countEl = getEl(`${key}-count`);
        const costEl = getEl(`${key}-cost`);
        const btn = getEl(`buy-${key}-btn`);
        if (countEl) countEl.innerText = gameState.collectors[key].count;
        if (costEl) costEl.innerText = Math.ceil(gameState.collectors[key].cost);
        if (btn) btn.disabled = gameState.stardust < gameState.collectors[key].cost;
    }

    const clickCostEl = getEl('click-upgrade-cost');
    const clickBtn = getEl('click-upgrade-btn');
    if (clickCostEl) clickCostEl.innerText = Math.ceil(gameState.upgrades.click.cost);
    if (clickBtn) clickBtn.disabled = gameState.stardust < gameState.upgrades.click.cost;

    // 視覺演化
    if (starHeart) {
        let stage = 1;
        if (gameState.starLevel > 50) stage = 4;
        else if (gameState.starLevel > 20) stage = 3;
        else if (gameState.starLevel > 5) stage = 2;
        starHeart.className = `star-stage-${stage}`;
        // 隨等級增加稍微放大星核
        const scale = 1 + (gameState.starLevel % 20) * 0.02;
        starHeart.style.transform = `scale(${scale})`;
    }
}

// 處理星核點擊
function handleStarClick() {
    gameState.stardust += gameState.clickPower;
    updateUI();
}

// 計算每秒產量 (SPS)
function calculateSPS() {
    let totalSPS = 0;
    for (const key in gameState.collectors) {
        const collector = gameState.collectors[key];
        totalSPS += collector.count * collector.production;
    }
    gameState.sps = totalSPS;
}

// 購買收集器
function buyCollector(type) {
    const collector = gameState.collectors[type];
    if (gameState.stardust >= collector.cost) {
        gameState.stardust -= collector.cost;
        collector.count++;
        // 價格增加：每單位 1.15 倍
        collector.cost = collector.baseCost * Math.pow(1.15, collector.count);
        calculateSPS();
        updateUI();
        return true;
    }
    return false;
}

// 強化點擊
function upgradeClick() {
    const upgrade = gameState.upgrades.click;
    if (gameState.stardust >= upgrade.cost) {
        gameState.stardust -= upgrade.cost;
        upgrade.level++;
        gameState.clickPower = upgrade.level;
        upgrade.cost = upgrade.cost * 2;
        updateUI();
        return true;
    }
    return false;
}

// 計算升級成本
function calculateLevelUpCost() {
    return 100 * Math.pow(1.5, gameState.starLevel - 1);
}

// 星核升級
function levelUp() {
    const cost = calculateLevelUpCost();
    if (gameState.stardust >= cost) {
        gameState.stardust -= cost;
        gameState.starLevel++;
        updateUI();
        return true;
    }
    return false;
}

// 遊戲主迴圈
let lastTick = Date.now();
function gameLoop() {
    const now = Date.now();
    const deltaTime = (now - lastTick) / 1000;
    lastTick = now;

    gameState.stardust += gameState.sps * deltaTime;
    updateUI();

    if (typeof requestAnimationFrame !== 'undefined') {
        requestAnimationFrame(gameLoop);
    } else {
        setTimeout(gameLoop, 100);
    }
}

// 初始化
if (typeof window !== 'undefined') {
    window.onload = () => {
        if (starHeart) starHeart.onclick = handleStarClick;
        if (levelUpBtn) levelUpBtn.onclick = levelUp;

        const clickUpgradeBtn = getEl('click-upgrade-btn');
        if (clickUpgradeBtn) clickUpgradeBtn.onclick = upgradeClick;

        if (getEl('buy-motes-btn')) getEl('buy-motes-btn').onclick = () => buyCollector('motes');
        if (getEl('buy-comets-btn')) getEl('buy-comets-btn').onclick = () => buyCollector('comets');
        if (getEl('buy-nebulas-btn')) getEl('buy-nebulas-btn').onclick = () => buyCollector('nebulas');

        gameLoop();
    };
}

// 匯出以供測試
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { gameState, handleStarClick, calculateSPS, buyCollector, upgradeClick, levelUp, calculateLevelUpCost, updateUI };
}

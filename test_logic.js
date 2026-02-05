const { gameState, handleStarClick, calculateSPS, buyCollector, upgradeClick, levelUp, calculateLevelUpCost } = require('./game.js');

function assert(condition, message) {
    if (!condition) {
        console.error('FAILED:', message);
        process.exit(1);
    }
    console.log('PASSED:', message);
}

// Reset state for clean tests
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

// Test 1: Clicking
handleStarClick();
assert(gameState.stardust === 1, 'Clicking should increase stardust by 1');

// Test 2: Level up
const levelCost = calculateLevelUpCost();
assert(levelCost === 100, 'Initial level up cost should be 100');
gameState.stardust = 100;
const leveled = levelUp();
assert(leveled === true, 'Should be able to level up with 100 stardust');
assert(gameState.starLevel === 2, 'Star level should be 2');
assert(gameState.stardust === 0, 'Stardust should be 0 after level up');

// Test 3: Other collectors
gameState.stardust = 100;
const boughtComet = buyCollector('comets');
assert(boughtComet === true, 'Should be able to buy comet with 100 stardust');
assert(gameState.collectors.comets.count === 1, 'Comet count should be 1');
calculateSPS();
assert(gameState.sps === 5, 'SPS should be 5 after buying one comet');

console.log('All logic tests passed!');

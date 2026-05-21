package com.ror.models;

import java.text.DecimalFormat;
import java.util.*;
import com.ror.models.Inventory.*;

public class Hero {
    private Inventory inventory;
    private int hp;
    private int attack;
    private int mana;
    private int defense;
    private int speed;
    private int level;
    private int experience = 0;
    private String name;
    private String charClass;
    private String weapon;
    private String skill1, skill2, ultimate;
    private int manaCostSkill1, manaCostSkill2, manaCostUltimate;
    private int cooldown1 = 0;
    private int cooldown2 = 0;
    private int cooldownU = 0; 
    private int skillCd1, skillCd2, skillCdU;
    private int manaCap, hpCap;
    private int gold;
    private int stunned = -1;
    private int poisoned = 0;

    public Hero(){ 

    }

    public Hero(int hp, int attack, int mana, int defense, int speed, int level, String name, String charClass, String weapon, String skill1, String skill2, String ultimate, int manaCostSkill1, int manaCostSkill2, int manaCostUltimate, int maxAtk, int maxMana, int maxDef) {
        this.hp = hp;
        this.attack = attack;
        this.mana = mana;
        this.defense = defense;
        this.speed = speed;
        this.level = level;
        this.name = name;
        this.weapon = weapon;
        this.charClass = charClass;
        this.skill1 = skill1;
        this.skill2 = skill2;
        this.ultimate = ultimate;
        this.manaCostSkill1 = manaCostSkill1;
        this.manaCostSkill2 = manaCostSkill2;
        this.manaCostUltimate = manaCostUltimate;
        setBaseStats(hp, attack, mana, defense);
        setMaxStats(maxAtk, maxMana, maxDef);
        this.inventory = new Inventory();
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void basicAttack(Hero hero, Entity enemy) {
        if(enemy.dodgeHeroAtk(enemy, hero)) return;

        int damage = multiplierB(getAttack(), getLevel());
        
        double manaRecovery = manaCap * 0.2;

        if(manaRecovery+getMana() > manaCap){
            setMana(manaCap);
        } else {
            int addMana = (int) manaRecovery + getMana();
            setMana(addMana);
        }

        int damageDealt = damage - enemy.getDefense()/2;

        System.out.println(getName() + " used Basic Attack!");
        System.out.println("Basic Attack deals " + damageDealt + " damage!");

        enemy.setHp(enemy.getHp() - damageDealt);
    }

    public void skill1(Hero hero, Entity enemy){
        setCooldown1(skillCd1);
        if(enemy.dodgeHeroAtk(enemy, hero)) return;

        int damage = multiplier1(getAttack(), getLevel());

        int scaledCost = (int)(manaCostSkill1 * (1 + 0.005 * (level - 1)));

        int manaReduce = getMana() - scaledCost;
        setMana(manaReduce);

        int damageDealt = damage - enemy.getDefense()/2;

        System.out.println(getName() + " used " + getSkill1() + "!");
        System.out.println(getSkill1() + " deals " + damageDealt + " damage!");

        enemy.setHp(enemy.getHp() - damageDealt);
    }

    public void skill2(Hero hero, Entity enemy){
        setCooldown2(skillCd2);
        if(enemy.dodgeHeroAtk(enemy, hero)) return;

        int damage = multiplier2(getAttack(), getLevel());

        int scaledCost = (int)(manaCostSkill2 * (1 + 0.005 * (level - 1)));
        
        int manaReduce = getMana() - scaledCost;
        setMana(manaReduce);

         int damageDealt = damage - enemy.getDefense()/2;

        System.out.println(getName() + " used " + getSkill2() + "!");
        System.out.println(getSkill2() + " deals " + damageDealt + " damage!");

        enemy.setHp(enemy.getHp() - damageDealt);
    }

    public void ultimate(Hero hero, Entity enemy){
        setCooldownU(skillCdU);

        int damage = multiplierU(getAttack(), getLevel());

        int scaledCost = (int)(manaCostUltimate * (1 + 0.005 * (level - 1)));

        int manaReduce = getMana() - scaledCost;
        setMana(manaReduce);

        int damageDealt = damage - enemy.getDefense()/2;

        System.out.println(getName() + " used " + getUltimate() + "!");
        System.out.println(getUltimate() + " deals " + damageDealt + " damage!");

        enemy.setHp(enemy.getHp() - damageDealt);
    }

    //damage multiplier methods
    public int multiplierB(int atk, int L) {
        double multiplier = 1.0 + Math.pow((L - 1) / 59.0, 2);
        double damageDealt = atk * multiplier;
        return (int) Math.round(damageDealt);
    }

    public int multiplier1(int atk, int L) {
        double multiplier = 1.0 + 1.5 * Math.pow((L - 1) / 59.0, 2);
        double damageDealt = atk * multiplier;
        return (int) Math.round(damageDealt);
    }

    public int multiplier2(int atk, int L) {
        double multiplier = 1.0 + 2.0 * Math.pow((L - 1) / 59.0, 2);
        double damageDealt = atk * multiplier;
        return (int) Math.round(damageDealt);
    }

    public int multiplierU(int atk, int L) {
        double multiplier = 1.0 + 2.5 * Math.pow((L - 1) / 59.0, 2);
        double damageDealt = atk * multiplier;
        return (int) Math.round(damageDealt);
    }

    // Mana Scale cost
    public int scaledCost(int manaCost){
        int scaledCost = (int)(manaCost * (1 + 0.005 * (getLevel() - 1)));
        
        return scaledCost;
    }

    // Dodge enemies attack method || Hero dodges
    public boolean dodgeEnemyAtk(Hero defender, Entity attacker){
        Random rand = new Random();
        double dodgeChance = (double) defender.getSpeed() / (defender.getSpeed() + attacker.getSpeed()) * 0.5;
        double roll = rand.nextDouble(0.0, 1.0);

        if (roll < dodgeChance) {
            System.out.println(defender.getName() + " swiftly dodged the attack!");
            return true;
        }

        return false;
    }

    // Getters
    public int getHp() {
        return hp;
    }

    public int getAttack() {
        return attack;
    }

    public int getMana() {
        return mana;
    }

    public int getDefense() {
        return defense;
    }

    public int getSpeed() {
        return speed;
    }

    public int getLevel() {
        return level;
    }

    public int getExperience() {
        return experience;
    }

    public String getName() {
        return name;
    }

    public String getCharClass() {
        return charClass;
    }

    public String getWeapon() {
        return weapon;
    }

    public String getSkill1() {
        return skill1;
    }

    public String getSkill2() {
        return skill2;
    }

    public String getUltimate() {
        return ultimate;
    }

    public int getManaCostSkill1(){
        return manaCostSkill1;
    }

    public int getManaCostSkill2(){
        return manaCostSkill2;
    }

    public int getManaCostUltimate(){
        return manaCostUltimate;
    }

    public int getCooldown1() {
        return cooldown1;
    }

    public int getCooldown2() {
        return cooldown2;
    }

    public int getCooldownU() {
        return cooldownU;
    }

    public int getHpCap() {
        return hpCap;
    }

    public int getManaCap() {
        return manaCap;
    }

    public int getStunned() {
        return stunned;
    }

    public int getPoison() {
        return poisoned;
    }

    // Setters
    public void setHp(int hp) {
        this.hp = hp;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public void setDefense(int defense) {
        this.defense = defense;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCharClass(String charClass) {
        this.charClass = charClass;
    }

    public void setWeapon(String weapon) {
        this.weapon = weapon;
    }

    public void setCooldown1(int cd1) {
        cooldown1 = cd1;
    }

    public void setCooldown2(int cd2) {
        cooldown2 = cd2;
    }

    public void setCooldownU(int cdU) {
        cooldownU = cdU;
    }

    public void setManaCap(int manaCap){
        this.manaCap = manaCap;
    }

    public void setHpCap(int hpCap){
        this.hpCap = hpCap;
    }

    public void setStun(int stunned){
        this.stunned = stunned;
    }

    public void setPoison(int poisoned) {
        this.poisoned = poisoned;
    }


    // BONUS stats (from training)
    private int bonusHp = 0;
    private int bonusAtk = 0;
    private int bonusDef = 0;
    private int bonusMana = 0;

    // Bonus application methods
    public void addBonusHp(int value) {
        bonusHp += value;
        setHp(getHp() + value);
    }

    public void addBonusAtk(int value) {
        bonusAtk += value;
        setAttack(getAttack() + value);
    }

    public void addBonusDef(int value) {
        bonusDef += value;
        setDefense(getDefense() + value);
    }

    public void addBonusMana(int value) {
        bonusMana += value;
        setMana(getMana() + value);
    }

    // level mechanism
    public void levelUp(int gainedExp){
        DecimalFormat df = new DecimalFormat("#,##0");

        int totalExp = this.experience + gainedExp;

        if(level >= 60){
            System.out.println("┌────────────────────────────────────┐");
            System.out.println("│     ALREADY AT MAX LEVEL [60]!     │");
            System.out.println("└────────────────────────────────────┘");

            this.experience = 0;
            
            return;
        }

        if(totalExp < 100) {
            this.experience = totalExp;
            return;
        }

        int levelGained = totalExp / 100;
        level += levelGained;

        if (level >= 60) {
            level = 60;
            totalExp = 0;
        } else {
            totalExp = totalExp % 100;
        }

        this.experience = totalExp;
        
        int newHp   = calculateStat(getBaseHp(), getMaxHp(), level) + bonusHp;
        int newAtk  = calculateStat(getBaseAtk(), getMaxAtk(), level) + bonusAtk;
        int newMana = calculateStat(getBaseMana(), getMaxMana(), level) + bonusMana;
        int newDef  = calculateStat(getBaseDef(), getMaxDef(), level) + bonusDef;


        // int newHp = calculateStat(getBaseHp(), getMaxHp(), level);
        // int newAtk = calculateStat(getBaseAtk(), getMaxAtk(), level);
        // int newMana = calculateStat(getBaseMana(), getMaxMana(), level);
        // int newDef = calculateStat(getBaseDef(), getMaxDef(), level);

        System.out.println();
        System.out.println("┌───────────────────────────────┐");
        System.out.println("│        Level Increased!       │");
        System.out.println("│    >>>  Stats Updated!  <<<   │");
        System.out.println("└───────────────────────────────┘");
        System.err.println();
        System.out.println(">>>>>     Current Level: " + level + "     <<<<<");
        System.out.println();
        System.out.println(String.format("> Health :  %7s   ->  %7s", df.format(getHp()), df.format(newHp)));
        System.out.println(String.format("> Attack :  %7s   ->  %7s", df.format(getAttack()), df.format(newAtk)));
        System.out.println(String.format("> Mana   :  %7s   ->  %7s", df.format(getMana()), df.format(newMana)));
        System.out.println(String.format("> Defense:  %7s   ->  %7s", df.format(getDefense()), df.format(newDef)));
        System.out.println();
        System.out.println(">>>>> Current Experience: " + this.experience+ "/100" + " <<<<<");
        System.out.println();

        setHp(newHp);
        setAttack(newAtk);
        setMana(newMana);
        setDefense(newDef);
    }

    // stats multiplier
    private int maxHp = 22000;
    private int maxAtk;
    private int maxMana;
    private int maxDef;
    private int baseHp;
    private int baseAtk;
    private int baseMana;
    private int baseDef;

    public void setBaseStats(int baseHp, int baseAtk, int baseMana, int baseDef){
        this.baseHp = baseHp;
        this.baseAtk = baseAtk;
        this.baseMana = baseMana;
        this.baseDef = baseDef;
    }

    public void setMaxStats(int maxAtk, int maxMana, int maxDef){
        this.maxAtk = maxAtk;
        this.maxMana = maxMana;
        this.maxDef = maxDef;
    }

    public void setMaxHp(int maxHp){
        this.maxHp = maxHp;
    }

    public void setMaxAtk(int maxAtk){
        this.maxAtk = maxAtk;
    }

    public void setMaxMana(int maxMana){
        this.maxMana = maxMana;
    }

    public void setMaxDef(int maxDef){
        this.maxDef = maxDef;
    }

    public int getBaseHp(){
        return baseHp;
    }

    public int getBaseAtk(){
        return baseAtk;
    }

    public int getBaseMana(){
        return baseMana;
    }

    public int getBaseDef(){
        return baseDef;
    }

    public int getMaxHp(){
        return maxHp;
    }

    public int getMaxAtk(){
        return maxAtk;
    }

    public int getMaxMana(){
        return maxMana;
    }

    public int getMaxDef(){
        return maxDef;
    }

    public static int calculateStat(int base, int max, int level) {
        double k = 1.3; // growth exponent
        int stat = (int) (base + (max - base) * Math.pow((double)(level - 1) / 59, k));
        return stat;
    }

    // gold mechanics
    public void setGold(int gold){
        this.gold = gold;
    }

    public int getGold(){
        return gold;
    }

    // Javines _________________________________________________________________________________________

    // Start Menu Progress _____________________________________________________________________________
    private boolean hasVisitedPrologue = false;

    public boolean hasVisitedPrologue(){
        return hasVisitedPrologue;
    }

    public void setHasVisitedPrologue(boolean done){
        this.hasVisitedPrologue = done;
    }

    // Main Menu Progress________________________________________________________________________________

    private boolean hasVisitedShop = false;
    private boolean hasOpenedInventory = false;
    private boolean hasVisitedAcademy = false;
    private boolean hasVisitedArea1 = false;
    private boolean hasVisitedArea2 = false;
    private boolean hasVisitedArea3 = false;

    public boolean hasVisitedShop(){
        return hasVisitedShop;
    }

    public void setHasVisitedShop(boolean done){
        this.hasVisitedShop = done;
    }

    public boolean hasOpenedInventory(){
        return hasOpenedInventory;
    }

    public void setHasOpenedInventory(boolean done){
        this.hasOpenedInventory = done;
    }

    public boolean hasVisitedAcademy(){
        return hasVisitedAcademy;
    }

    public void setHasVisitedAcademy(boolean done){
        this.hasVisitedAcademy = done;
    }

    public boolean hasVisitedArea1(){
        return hasVisitedArea1;
    }

    public void setHasVisitedArea1(boolean done){
        this.hasVisitedArea1 = done;
    }

    public boolean hasVisitedArea2(){
        return hasVisitedArea2;
    }

    public void setHasVisitedArea2(boolean done){
        this.hasVisitedArea2 = done;
    }

    public boolean hasVisitedArea3(){
        return hasVisitedArea3;
    }

    public void setHasVisitedArea3(boolean done){
        this.hasVisitedArea3 = done;
    }

    //Academy Progress____________________________________________________________________________________

    private boolean hasVisitedGym = false;
    private boolean hasVisitedLibrary = false;
    private boolean hasVisitedOffice = false;

    public boolean hasVisitedLibrary() { 
        return hasVisitedLibrary; 
    }
    public void setVisitedLibrary(boolean visited) { 
        this.hasVisitedLibrary = visited; 
    }

    public boolean hasVisitedGym() { 
        return hasVisitedGym; 
    }

    public void setVisitedGym(boolean visited) { 
        this.hasVisitedGym = visited; 
    }

    public boolean hasVisitedOffice() { 
        return hasVisitedOffice; 
    }
    public void setVisitedOffice(boolean visited) { 
        this.hasVisitedOffice = visited; 
    }

    // Library Quest Progress_____________________________________________________________________________

    private boolean libraryQuest1Done = false;
    private boolean libraryQuest2Done = false;
    private boolean riddle1Done = false;
    private boolean riddle2Done = false;
    private boolean riddle3Done = false;
    private boolean hasVisitedRiddles = false;
    private boolean hasVisitedBookFinding = false;

    public boolean isLibraryQuest1Done() { 
        return libraryQuest1Done; 
    }

    public void setLibraryQuest1Done(boolean done) { 
        this.libraryQuest1Done = done; 
    }

    public boolean isLibraryQuest2Done() { 
        return libraryQuest2Done; 
    }

    public void setLibraryQuest2Done(boolean done) { 
        this.libraryQuest2Done = done; 
    }

    public boolean isRiddle1Done(){
        return riddle1Done;
    }

    public boolean isRiddle2Done(){
        return riddle2Done;
    }

    public boolean isRiddle3Done(){
        return riddle3Done;
    }

    public void setRiddle1Done(boolean done){
        this.riddle1Done = done;
    }

    public void setRiddle2Done(boolean done){
        this.riddle2Done = done;
    }

    public void setRiddle3Done(boolean done){
        this.riddle3Done = done;
    }

    public boolean isAllRiddlesDone() {
        return riddle1Done && riddle2Done && riddle3Done;
    }

    public boolean isRiddleQuestDone() {
        return isAllRiddlesDone();
    }

    public void setVisitedRiddles(boolean done) {
        this.hasVisitedRiddles = done;
    }

    public boolean hasVisitedRiddles() {
        return hasVisitedRiddles;
    }

    public void setVisitedBookFinding(boolean done) {
        this.hasVisitedBookFinding = done;
    }

    public boolean hasVisitedBookFinding() {
        return hasVisitedBookFinding;
    }

    // Area Progress
    // Office Progress______________________________________________________________________________
    private boolean canEnterArea1 = false; 
    private boolean canEnterArea2 = false;
    private boolean canEnterArea3 = false;
    private boolean haveEntered = false;

    public boolean haveEntered() {
        return haveEntered;
    }

    public void setHaveEntered(boolean done) {
        this.haveEntered = done;
    }

    public boolean canEnterArea1() {
        return canEnterArea1; 
    }

    public boolean canEnterArea2() { 
        return canEnterArea2; 
    }

    public boolean canEnterArea3() { 
        return canEnterArea3; 
    }

    public void unlockArea1(boolean canEnterArea1) { 
        this.canEnterArea1 = canEnterArea1; 
    }

    public void unlockArea2(boolean canEnterArea2) { 
        this.canEnterArea2 = canEnterArea2; 
    }

    public void unlockArea3(boolean canEnterArea3) { 
        this.canEnterArea3 = canEnterArea3; 
    }

    // Area unlock tracking in Hero class
    private boolean unlockedArea1 = false;
    private boolean unlockedArea2 = false;
    private boolean unlockedArea3 = false;

    // --- Setters ---
    public void setUnlockArea1(boolean status) {
        this.unlockedArea1 = status;
    }

    public void setUnlockArea2(boolean status) {
        this.unlockedArea2 = status;
    }

    public void setUnlockArea3(boolean status) {
        this.unlockedArea3 = status;
    }

    // --- Getters ---
    public boolean hasUnlockedArea1() {
        return this.unlockedArea1;
    }

    public boolean hasUnlockedArea2() {
        return this.unlockedArea2;
    }

    public boolean hasUnlockedArea3() {
        return this.unlockedArea3;
    }


    // Modified Area Progress _________________________________________________________________________

    private boolean haveDefeatedArea1Boss = false; 
    private boolean haveDefeatedArea2Boss = false; 
    private boolean haveDefeatedArea3Boss = false; 

    public void setHaveDefeatedArea1Boss(boolean haveDefeatedArea1Boss) {
        this.haveDefeatedArea1Boss = haveDefeatedArea1Boss;
    }

    public void setHaveDefeatedArea2Boss(boolean haveDefeatedArea2Boss) {
        this.haveDefeatedArea2Boss = haveDefeatedArea2Boss;
    }

    public boolean getHaveDefeatedArea1Boss() {
        return haveDefeatedArea1Boss;
    }

    public boolean getHaveDefeatedArea2Boss () {
        return haveDefeatedArea2Boss;
    }

    public void setHaveDefeatedArea3Boss(boolean haveDefeatedArea3Boss) { 
        this.haveDefeatedArea3Boss = haveDefeatedArea3Boss;
    }

    public boolean getHaveDefeatedArea3Boss() { 
        return haveDefeatedArea3Boss;
    }
    

    // Training progress_______________________________________________________________________________
    private boolean finishedEndurance = false;
    private boolean finishedStrength = false;
    private boolean finishedDurability = false;
    private boolean finishedManaRefinement = false;
    private int numberOfTrainingFinished = 0;
    private boolean finishedAllTraining = false; 
    private boolean haveExploredButExited = false;
    private boolean haveAcceptedButExited = false;

    public boolean hasFinishedEndurance() { 
        return finishedEndurance; 
    }

    public void setFinishedEndurance(boolean finishedEndurance) { 
        this.finishedEndurance = finishedEndurance; 
    }

    public boolean hasFinishedStrength() { 
        return finishedStrength; 
    }

    public void setFinishedStrength(boolean finishedStrength) { 
        this.finishedStrength = finishedStrength; 
    }

    public boolean hasFinishedDurability() { 
        return finishedDurability; 
    }

    public void setFinishedDurability(boolean finishedDurability) { 
        this.finishedDurability = finishedDurability; 
    }

    public boolean hasFinishedManaRefinement() {
         return finishedManaRefinement; 
    }

    public void setFinishedManaRefinement(boolean finishedManaRefinement) { 
        this.finishedManaRefinement = finishedManaRefinement; 
    }

    public int getNumberOfTrainingFinished() { 
        return numberOfTrainingFinished; 
    }

    public void setNumberOfTrainingFinished(int numberOfTrainingFinished) { 
        this.numberOfTrainingFinished = numberOfTrainingFinished; 
    }

    public boolean hasFinishedAllTraining(){
        return finishedAllTraining;
    }

    public void setFinishedAllTraining(boolean finishedAllTraining){
        this.finishedAllTraining = finishedAllTraining;
    }

    public boolean haveExploredButExited(){
        return haveExploredButExited;
    }

    public void setHaveExploredButExited(boolean done){
        this.haveExploredButExited = done;
    }

    public boolean haveAcceptedButExited(){
        return haveAcceptedButExited;
    }

    public void setHaveAcceptedButExited(boolean done){
        this.haveAcceptedButExited = done;
    }

    // Shop Related ________________________________________________________________________________________________________________________
    private boolean hasConversedWithShop = false;

    public void setConversedWithShop(boolean done) {
        this.hasConversedWithShop = done;
    }

    public boolean getConversedWithShop(){
        return hasConversedWithShop;
    }

    // added while ga debug

    private boolean currentlyTraining;

    public boolean isCurrentlyTraining() {
        return currentlyTraining;
    }

    public void setCurrentlyTraining(boolean currentlyTraining) {
        this.currentlyTraining = currentlyTraining;
    }


    // Stats Check Related ___________________________________________________________________________________________________________________

    private boolean swordmanCharacterChosen = false;
    private boolean gunnerCharacterChosen = false;
    private boolean mageCharacterChosen = false;
    private long leaderboardRunStartedAtMillis = 0L;
    private long leaderboardAccumulatedMillis = 0L;
    private long leaderboardSessionStartedAtMillis = 0L;
    private long leaderboardArea1ClearMillis = 0L;
    private long leaderboardArea2ClearMillis = 0L;
    private long leaderboardArea3ClearMillis = 0L;
    private boolean leaderboardRecorded = false;

    public void setSwordmanCharacterChosen(boolean done) {
        this.swordmanCharacterChosen = done;
    }

    public boolean getSwordmanCharacterChosen() {
        return swordmanCharacterChosen;
    }

    public void setGunnerCharacterChosen(boolean done) {
        this.gunnerCharacterChosen = done;
    }

    public boolean getGunnerCharacterChosen() {
        return gunnerCharacterChosen;
    }

    public void setMageCharacterChosen(boolean done) {
        this.mageCharacterChosen = done;
    }

    public boolean getMageCharacterChosen() {
        return mageCharacterChosen;
    }

    public void resetCharacterFlags() { // not implemented
        this.setSwordmanCharacterChosen(false);
        this.setMageCharacterChosen(false);
        this.setGunnerCharacterChosen(false);
    }

    public long getLeaderboardRunStartedAtMillis() {
        return leaderboardRunStartedAtMillis;
    }

    public void setLeaderboardRunStartedAtMillis(long leaderboardRunStartedAtMillis) {
        this.leaderboardRunStartedAtMillis = leaderboardRunStartedAtMillis;
    }

    public long getLeaderboardAccumulatedMillis() {
        return leaderboardAccumulatedMillis;
    }

    public void setLeaderboardAccumulatedMillis(long leaderboardAccumulatedMillis) {
        this.leaderboardAccumulatedMillis = leaderboardAccumulatedMillis;
    }

    public long getLeaderboardSessionStartedAtMillis() {
        return leaderboardSessionStartedAtMillis;
    }

    public void setLeaderboardSessionStartedAtMillis(long leaderboardSessionStartedAtMillis) {
        this.leaderboardSessionStartedAtMillis = leaderboardSessionStartedAtMillis;
    }

    public long getLeaderboardArea1ClearMillis() {
        return leaderboardArea1ClearMillis;
    }

    public void setLeaderboardArea1ClearMillis(long leaderboardArea1ClearMillis) {
        this.leaderboardArea1ClearMillis = leaderboardArea1ClearMillis;
    }

    public long getLeaderboardArea2ClearMillis() {
        return leaderboardArea2ClearMillis;
    }

    public void setLeaderboardArea2ClearMillis(long leaderboardArea2ClearMillis) {
        this.leaderboardArea2ClearMillis = leaderboardArea2ClearMillis;
    }

    public long getLeaderboardArea3ClearMillis() {
        return leaderboardArea3ClearMillis;
    }

    public void setLeaderboardArea3ClearMillis(long leaderboardArea3ClearMillis) {
        this.leaderboardArea3ClearMillis = leaderboardArea3ClearMillis;
    }

    public boolean isLeaderboardRecorded() {
        return leaderboardRecorded;
    }

    public void setLeaderboardRecorded(boolean leaderboardRecorded) {
        this.leaderboardRecorded = leaderboardRecorded;
    }

    //Reset________________________________________________________________________________________________________________________

    public void resetAllProgress() {
    // Reset stats
    this.hp = this.baseHp;
    this.attack = this.baseAtk;
    this.mana = this.baseMana;
    this.defense = this.baseDef;
    this.speed = 0;
    this.level = 1;
    this.experience = 0;
    this.gold = 0;

    // Reset weapon and skills
    this.weapon = null;
    this.skill1 = null;
    this.skill2 = null;
    this.ultimate = null;
    this.manaCostSkill1 = 0;
    this.manaCostSkill2 = 0;
    this.manaCostUltimate = 0;
    this.cooldown1 = 0;
    this.cooldown2 = 0;
    this.cooldownU = 0;

    // Reset status effects
    this.stunned = -1;
    this.poisoned = 0;

    // Reset base and max stats
    this.maxHp = 22000;
    this.maxAtk = 0;
    this.maxMana = 0;
    this.maxDef = 0;

    // Reset start/menu progress
    this.hasVisitedPrologue = false;
    this.hasVisitedShop = false;
    this.hasOpenedInventory = false;
    this.hasVisitedAcademy = false;
    this.hasVisitedArea1 = false;
    this.hasVisitedArea2 = false;
    this.hasVisitedArea3 = false;

    // Reset academy progress
    this.hasVisitedGym = false;
    this.hasVisitedLibrary = false;
    this.hasVisitedOffice = false;

    // Reset library quests
    this.libraryQuest1Done = false;
    this.libraryQuest2Done = false;
    this.riddle1Done = false;
    this.riddle2Done = false;
    this.riddle3Done = false;

    // Reset area progress
    this.canEnterArea1 = false;
    this.canEnterArea2 = false;
    this.canEnterArea3 = false;
    this.haveEntered = false;
    this.haveDefeatedArea1Boss = false;
    this.haveDefeatedArea2Boss = false;
    this.haveDefeatedArea3Boss = false;
    this.unlockedArea1 = false;
    this.unlockedArea2 = false;
    this.unlockedArea3 = false;

    // Reset training progress
    this.finishedEndurance = false;
    this.finishedStrength = false;
    this.finishedDurability = false;
    this.finishedManaRefinement = false;
    this.numberOfTrainingFinished = 0;
    this.finishedAllTraining = false;
    this.haveExploredButExited = false;
    this.haveAcceptedButExited = false;

    // Reset shop interactions
    this.hasConversedWithShop = false;

    // Reset character choice flags
    resetCharacterFlags();

    // Reset leaderboard tracking
    this.leaderboardRunStartedAtMillis = 0L;
    this.leaderboardAccumulatedMillis = 0L;
    this.leaderboardSessionStartedAtMillis = 0L;
    this.leaderboardArea1ClearMillis = 0L;
    this.leaderboardArea2ClearMillis = 0L;
    this.leaderboardArea3ClearMillis = 0L;
    this.leaderboardRecorded = false;
}


 
}

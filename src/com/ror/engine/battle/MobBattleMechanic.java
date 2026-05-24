package com.ror.engine.battle;

import com.ror.models.*;
import java.text.DecimalFormat;
import java.util.*;

// This only works for mobs
public class MobBattleMechanic {
    static Scanner scanner = new Scanner(System.in);
    static Random rand = new Random();
    private int origHp;
    private int origMana;
    int round = 1;
    public static boolean run = false;
    DecimalFormat df = new DecimalFormat("#,##0");

    private javax.swing.Timer turnTimer;
    private int remainingSeconds = 30;

    

    public boolean fight(Hero player, Entity enemy) {
        enemy.setManaCap(enemy.getMana());
        setOriginalStats(player);
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("<<==================================================================>>");
        System.out.println();
        System.out.println("                      "+ player.getName() +"");
        System.out.println();
        System.out.println("                                 VS               ");
        System.out.println();
        System.out.println("                                  "+ enemy.getName() +"");
        System.out.println();
        System.out.println("<<==================================================================>>");
        System.out.println();
        System.out.println("\t\t       ┌────────────────────┐");
        System.out.println("\t\t       │       FIGHT!       │");
        System.out.println("\t\t       └────────────────────┘");
        int choice = -1;

        while (player.getHp() > 0 && enemy.getHp() > 0) {
            while (true) {
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println("\nPlayer HP: " + df.format(player.getHp()) +  " | Player Mana: " + df.format(player.getMana()) +  " || Enemy HP: " + df.format(enemy.getHp()) + " | Enemy Mana: " + df.format(enemy.getMana()));
                System.out.println("+--------------------------------   ROUND " + round + "  ------------------------------------+");
                System.out.println("Choose your attack:");
                System.out.println();
                System.out.println("   [ 1 ] Basic Attack");
                System.out.println("   [ 2 ] Skill 1 - "+ player.getSkill1() + " (Mana Cost: " + player.scaledCost(player.getManaCostSkill1()) + ") (Cooldown: " + player.getCooldown1() + ")");
                System.out.println("   [ 3 ] Skill 2 - "+ player.getSkill2() + " (Mana Cost: " + player.scaledCost(player.getManaCostSkill2()) + ") (Cooldown: " + player.getCooldown2() + ")");
                System.out.println("   [ 4 ] Ultimate - "+ player.getUltimate() + " (Mana Cost: " + player.scaledCost(player.getManaCostUltimate()) + ") (Cooldown: " + player.getCooldownU() + ")");
                System.out.println("   [ 5 ] Inventory");
                System.out.println("   [ 6 ] Run Away!");
                System.out.println("+--------------------------------------------------------------------------------+");
                System.out.print(">>> ");

                try {
                    choice = Integer.parseInt(scanner.nextLine().trim());
                    if (choice >= 1 && choice <= 6) break;
                    else System.out.println("\nPlease select a valid choice [1-6]\n\n\n");
                } catch (Exception e) {
                    System.out.println("\nInvalid input! Please enter a number between 1 and 6.\n\n\n");
                }
            }

            System.out.println();
            boolean valid = true;
            System.out.println();
            System.out.println("<<==========================================================================>>");
            System.out.println();
            System.out.println("Player's Turn:");
            if(choice == 5) {
                    player.getInventory().useItem(player);
                    System.out.println();
                    System.out.println("<<==========================================================================>>");
                    System.out.println();
                    continue;
                }
            if (player.getStunned() <= 0) {
                if(choice == 6) {
                    if (runAway(player, enemy)) {
                        restoreStats(player);
                        return false;
                    }
                }

                valid = castAttack(player, enemy, choice);

                if (!valid) {
                    // Skip enemy turn, let player retry instead
                    System.out.println("\n<<==========================================================================>>");
                    continue;
                }
            }

            reducePlayerNegativeEffects(player);
            reducePlayerCooldown(player);

            // Check if enemy is defeated
            if (enemy.getHp() <= 0) {
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println("┌────────────────────────────────────────┐");
                System.out.println("│        Enemy has been defeated!        │");
                System.out.println("└────────────────────────────────────────┘");
                System.out.println();
                System.out.println();
                System.out.println("<<==========================================================================>>\n\n");

                restoreStats(player);
                return true;
            }

            System.out.println();

            // Enemy's turn
            System.out.println("Enemy's Turn:");
            if (enemyValid(enemy)) {
                enemyCastAttack(player, enemy);
            }
            reduceEnemyNegativeEffects(enemy);
            reduceEnemyCooldown(enemy);
            
            System.out.println();
            System.out.println("<<==========================================================================>>");
            System.out.println();
            System.out.println();
            System.out.println();

            

            // Check if player is defeated
            if (player.getHp() <= 0) {
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println("\t\t  ┌────────────────────────────────────────┐");
                System.out.println("\t\t  │        You have been defeated!         │");
                System.out.println("\t\t  └────────────────────────────────────────┘");
                System.out.println();
                System.out.println();
                System.out.println();

                restoreStats(player);
                break;
            }

            round++;
            System.out.println();
        }
        return false;
    }

    public static boolean castAttack(Hero player, Entity enemy, int choice) {
        switch (choice) {
            case 1:
                player.basicAttack(player, enemy);
                break;

            case 2:
                if (player.getMana() < player.scaledCost(player.getManaCostSkill1())) {
                    System.out.println("Not enough mana!");
                    return false;
                }
                if (player.getCooldown1() == 0) {
                    player.skill1(player, enemy);
                } else {
                    System.out.println("Skill 1 (" + player.getSkill1() + ") is on cooldown for " + player.getCooldown1() + " more turn(s).");
                    return false;
                }
                break;

            case 3:
                if (player.getMana() < player.scaledCost(player.getManaCostSkill2())) {
                    System.out.println("Not enough mana!");
                    return false;
                }
                if (player.getCooldown2() == 0) {
                    player.skill2(player, enemy);
                } else {
                    System.out.println("Skill 2 (" + player.getSkill2() + ") is on cooldown for " + player.getCooldown2() + " more turn(s).");
                    return false;
                }
                break;

            case 4:
                if (player.getMana() < player.scaledCost(player.getManaCostUltimate())) {
                    System.out.println("Not enough mana!");
                    return false;
                }
                if (player.getCooldownU() == 0) {
                    player.ultimate(player, enemy);
                } else {
                    System.out.println("Ultimate (" + player.getUltimate() + ") is on cooldown for " + player.getCooldownU() + " more turn(s).");
                    return false;
                }
                break;
            
            case 5:
                // inventory
                break;
                
            case 6:
                // run away
                break;

            default:
                System.out.println("Invalid choice. Please select a valid attack.");
                return false;
        }

        return true;
    }

    public static void enemyCastAttack(Hero player, Entity enemy) {
        while (true) { // loop until valid action
            int choice = rand.nextInt(3) + 1; // Random 1–3

            switch (choice) {
                case 1:
                    enemy.basicAttack(enemy, player);
                    break;

                case 2:
                    if (enemy.getMana() >= enemy.getManaCostSkill1() && enemy.getCooldown1() == 0) {
                        enemy.skill1(enemy, player);
                    } else continue;
                    break;

                case 3:
                    if (enemy.getMana() >= enemy.getManaCostSkill2() && enemy.getCooldown2() == 0) {
                        enemy.skill2(enemy, player);
                    } else continue;
                    break;
            }
            break; // valid attack done
        }
    }

    public static void reducePlayerCooldown(Hero player) {
        if (player.getCooldown1() > 0) player.setCooldown1(player.getCooldown1() - 1);
        if (player.getCooldown2() > 0) player.setCooldown2(player.getCooldown2() - 1);
        if (player.getCooldownU() > 0) player.setCooldownU(player.getCooldownU() - 1);
    }

    public static void reduceEnemyCooldown(Entity enemy) {
        if (enemy.getCooldown1() > 0) enemy.setCooldown1(enemy.getCooldown1() - 1);
        if (enemy.getCooldown2() > 0) enemy.setCooldown2(enemy.getCooldown2() - 1);
    }

    public void setOriginalStats(Hero player) {
        this.origHp = player.getHp();
        this.origMana = player.getMana();
        player.setHp(player.getHp());
        player.setManaCap(player.getMana());
    }

    public void restoreStats(Hero player) {
        player.setHp(origHp);
        player.setMana(origMana);
        player.setCooldown1(0);
        player.setCooldown2(0);
        player.setCooldownU(0);
        player.setStun(0);
        player.setPoison(0);
        round = 1;
    }

    public static boolean enemyValid(Entity enemy) {
        if (enemy.getStunned() > 0) {
            System.out.println(enemy.getName() + " is stunned and cannot move! (Stun " + enemy.getStunned() + ")");
            return false;
        }

        if (enemy.getDisabled() > 0) {
            System.out.println(enemy.getName() + " is exhausted and cannot move! (Exhausted " + enemy.getDisabled() + ")");
            return false;
        }

        return true;
    }

    public void reducePlayerNegativeEffects(Hero player) {
        if (player.getStunned() > 0) {
            System.out.println(player.getName() + " is stunned for " + player.getStunned() + " turn(s)!");
        }

        if (player.getPoison() > 0) {
            int poisonDmg = (int) (player.getHp() * 0.05);
            player.setHp(player.getHp() - poisonDmg);
            System.out.println(player.getName() + " suffers " + poisonDmg + " poison damage!");
            player.setPoison(player.getPoison() - 1);
        }

        if (player.getStunned() >= 0) player.setStun(player.getStunned() - 1);
        if (player.getStunned() < 0) player.setStun(-1);
        if (player.getPoison() < 0) player.setPoison(0);

        if(player.getStunned() == 0) System.out.println(player.getName() + " is no longer stunned and can attack next round.");
    }

    public void reduceEnemyNegativeEffects(Entity enemy) {
        if (enemy.getStunned() >= 0) enemy.setStun(enemy.getStunned() - 1);
        if (enemy.getDisabled() >= 0) enemy.setDisabled(enemy.getDisabled() - 1);

        if (enemy.getStunned() < 0) enemy.setStun(-1);
        if (enemy.getDisabled() < 0) enemy.setDisabled(-1);

        if(enemy.getStunned() == 0) System.out.println(enemy.getName() + " is no longer stunned and can attack next round.");
        if(enemy.getDisabled() == 0) System.out.println(enemy.getName() + " is no longer exhausted and can attack next round.");
    }

    public boolean runAway(Hero player, Entity enemy) {
        System.out.println(">>> " + player.getName() + " attempts to run away! <<<");

        // Example formula: speed difference + randomness
        double baseChance = 30; // base 30% success rate
        double speedFactor = (player.getSpeed() - enemy.getSpeed()) * 0.5; // 0.5% per speed point difference
        double successChance = Math.min(90, Math.max(10, baseChance + speedFactor)); // clamp between 10–90%

        double roll = Math.random() * 100;

        if (roll <= successChance) {
            System.out.println("You successfully escaped from " + enemy.getName() + "!\n");
            run = true;
            System.out.println("<<==========================================================================>>\n\n");
            return true;
        } else {
            System.out.println("Failed to escape! " + enemy.getName() + " blocks your path!");
            return false;
        }
    }
}

package nachos.threads;

import nachos.ag.BoatGrader;

public class Boat {
    static BoatGrader bg;


    private static final int positionOahu = 0;
    private static final int positionMolokai = 1;

    private static int positionBoat;

    private static Lock lockForBoat;

    private static Condition condBoat, condPilot;

    private static int countChildrenOnOahu, countAdultsOnOahu, countChildrenOnMolokai, countAdultsOnMolokai, countOnBoat;

    private static boolean flagUnfinished;

    public static void selfTest() {
        BoatGrader b = new BoatGrader();

        System.out.println("\n***Testing Boats with only 2 children***");
        begin(0, 2, b);
        System.out.println("***Testing Boats with only 2 children finished***");

        System.out.println("\n***Testing Boats with 2 children, 1 adult***");
        begin(1, 2, b);
        System.out.println("***Testing Boats with 2 children, 1 adult finished***");

        System.out.println("\n***Testing Boats with 3 children, 3 adults***");
        begin(3, 3, b);
        System.out.println("***Testing Boats with 3 children, 3 adults finished***");
    }

    public static void begin(int adults, int children, BoatGrader b) {
        // Store the externally generated autograder in a class
        // variable to be accessible by children.
        bg = b;

        // Instantiate global variables here
        positionBoat = positionOahu;
        lockForBoat = new Lock();

        condPilot = new Condition(lockForBoat);
        condBoat = new Condition(lockForBoat);

        countAdultsOnMolokai = 0;
        countAdultsOnOahu = adults;
        countChildrenOnMolokai = 0;
        countChildrenOnOahu = children;
        countOnBoat = 0;
        flagUnfinished = true;

        // Create threads here. See section 3.4 of the Nachos for Java
        // Walkthrough linked from the projects page.

        Runnable childR = new Runnable() {
            public void run() {
                ChildItinerary();
            }
        };
        Runnable adultR = new Runnable() {
            public void run() {
                AdultItinerary();
            }
        };
        KThread[] childrenThread = new KThread[children];

        KThread[] adultsThread = new KThread[adults];
        for (int i = 0; i < adults; i++) {
            adultsThread[i] = new KThread(adultR);
            adultsThread[i].setName("Adult " + i);
            adultsThread[i].fork();
        }

        for (int i = 0; i < children; i++) {
            childrenThread[i] = new KThread(childR);
            childrenThread[i].setName("Child " + i);
            childrenThread[i].fork();
        }

        for (int i = 0; i < children; i++) {
            childrenThread[i].join();
        }

        for (int i = 0; i < adults; i++) {
            adultsThread[i].join();
        }

    }

    static void AdultItinerary() {
        bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
        //DO NOT PUT ANYTHING ABOVE THIS LINE.

	/* This is where you should put your solutions. Make calls
       to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
        int position = positionOahu;
        while (flagUnfinished) {
            if (position == positionOahu) {
                lockForBoat.acquire();
                while (positionBoat != positionOahu) {
                    condBoat.sleep();
                }
                if (!flagUnfinished) {
                    lockForBoat.release();
                    return;
                }
                if (countChildrenOnOahu <= 1) {
                    if (countOnBoat == 0) {
                        countAdultsOnOahu--;
                        countOnBoat++;
                        if (countChildrenOnOahu + countAdultsOnOahu == 0) {
                            flagUnfinished = false;
                        }
                        bg.AdultRowToMolokai();
                        positionBoat = positionMolokai;
                        position = positionMolokai;
                        countAdultsOnMolokai++;
                        countOnBoat--;
                        condBoat.wakeAll();
                    }
                }
                lockForBoat.release();
                KThread.yield();
            } else {
                lockForBoat.acquire();
                while (positionBoat != positionMolokai) {
                    condBoat.sleep();
                }
//                System.out.println("adult Molokai activated");
                if (!flagUnfinished) {
//                    System.out.println("Adult finished");
                    lockForBoat.release();
                    return;
                }
                if (countChildrenOnMolokai == 0) {
                    if (countOnBoat == 0) {
                        countOnBoat++;
                        countAdultsOnMolokai--;
                        bg.AdultRowToOahu();
                        position = positionOahu;
                        positionBoat = positionOahu;
                        countAdultsOnOahu++;
                        countOnBoat--;
                        condBoat.wakeAll();
                    }
                }
                lockForBoat.release();
                KThread.yield();
            }
        }
        System.out.println("Adult finished");
    }

    static void ChildItinerary() {
        bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
        //DO NOT PUT ANYTHING ABOVE THIS LINE.
        int position = positionOahu;
        while (flagUnfinished) {
            if (position == positionOahu) {
                lockForBoat.acquire();
                while (positionBoat != positionOahu) {
                    condBoat.sleep();
                }
//                System.out.println("child Oahu activated");
                if (!flagUnfinished) {
                    lockForBoat.release();
//                    System.out.println("A child finished");
                    return;
                }

                if (countOnBoat == 0) {
                    countOnBoat++;
                    countChildrenOnOahu--;
//                    System.out.println(countChildrenOnOahu + " children on Oahu, " + countAdultsOnOahu + " adults on Oahu");
                    if (countChildrenOnOahu == 0) {
                        if (countAdultsOnOahu == 0) {
                            flagUnfinished = false;
                        }
                        bg.ChildRowToMolokai();
                        position = positionMolokai;
                        positionBoat = positionMolokai;
                        countChildrenOnMolokai += 1;
                        countOnBoat = 0;
                        condBoat.wakeAll();
                    } else {
                        bg.ChildRowToMolokai();
                        condPilot.sleep();
//                        System.out.println("pilot ok");
                        position = positionMolokai;
                        condBoat.wakeAll();
                    }
                } else if (countOnBoat == 1) {
                    countOnBoat++;
                    countChildrenOnOahu--;
                    if (countChildrenOnOahu + countAdultsOnOahu == 0) {
                        flagUnfinished = false;
                    }
                    bg.ChildRideToMolokai();
                    position = positionMolokai;
                    positionBoat = positionMolokai;
                    countChildrenOnMolokai += 2;
                    countOnBoat=0;
//                    System.out.println("passenger ok");
                    condPilot.wakeAll();
                }
                lockForBoat.release();
                KThread.yield();
            } else {
                lockForBoat.acquire();
                while (positionBoat != positionMolokai) {
                    condBoat.sleep();
                }
//                System.out.println("child Molokai activated");
                if (!flagUnfinished) {
//                    System.out.println("A child finished");
                    lockForBoat.release();
                    return;
                }
                if (countOnBoat == 0) {
                    countOnBoat++;
                    countChildrenOnMolokai--;

                    bg.ChildRowToOahu();
                    positionBoat = positionOahu;
                    position = positionOahu;

                    countChildrenOnOahu++;
                    countOnBoat--;
//                    System.out.println(countChildrenOnOahu + " children on Oahu, " + countAdultsOnOahu + " adults on Oahu");
                    condBoat.wakeAll();
                }
                lockForBoat.release();
                KThread.yield();
            }
        }
        System.out.println("A child finished");
    }

    static void SampleItinerary() {
        // Please note that this isn't a valid solution (you can't fit
        // all of them on the boat). Please also note that you may not
        // have a single thread calculate a solution and then just play
        // it back at the autograder -- you will be caught.
        System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
        bg.AdultRowToMolokai();
        bg.ChildRideToMolokai();
        bg.AdultRideToMolokai();
        bg.ChildRideToMolokai();
    }

}

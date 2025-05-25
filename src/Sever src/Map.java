import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Map {
	int width;	
	int num_mine;
	int[][] mineMap;
	int[][] displayMap;
	int remainingMines;
	HashMap<Integer, Integer> minePosition;  
	
	public Map(int width, int num_mine) {
		this.width = width;
		this.num_mine = num_mine;
		
		// create map
		System.out.println("Create  "+ width+" X "+ width + "  map");
		mineMap = new int[width][width];
		displayMap = new int[width][width];
		for (int i=0; i<width*width; i++) {
			mineMap[i/width][i%width] = 0;
			displayMap[i/width][i%width] = 0;
		}
		
		// create mines
		System.out.println("Create  "+num_mine+"  mines");
		Random r = new Random();
		minePosition = new HashMap<>(); 
		for (int i = 0; i < num_mine; i++) {
			int position = r.nextInt(width * width);
			while (minePosition.containsValue(position))   // check repetition
				position = r.nextInt(width * width);			
			minePosition.put(i, position);
		}
		
		// deploy mines
		System.out.println("mine positions");
		for (int i = 0; i < num_mine; i++) {
			int x = minePosition.get(i) / width;
			int y = minePosition.get(i) % width;
			System.out.println(x+", "+y);
			mineMap[x][y] = 1;
		}
		
		printMap(mineMap);
		
	}

		
	public int checkMine(int x, int y) {
		int pos = (x*width) + y;
		
		if (minePosition.containsValue(pos)) {
			//System.out.println("   Find mine at ("+x+", "+y+")");		
			return pos;
		}
		else { 
			//System.out.println("   No mine at ("+x+", "+y+")");
			return -1;
		}
		
	}

	public void printMap(int[][] a) {
		System.out.println();
        for (int i = 0; i < a.length; i++) {
            System.out.print(a[i][0]);
            for (int j = 1; j < a[0].length; j++) 
                System.out.print(" " + a[i][j]);            
            System.out.println();
        }
    }
	
	public void updateMap(int x, int y) {
		displayMap[x][y]=1;
		mineMap[x][y] = 0;
//		printMap(displayMap);
	}
		
	
}

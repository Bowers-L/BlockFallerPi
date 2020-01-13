/*

/**************************************\
 			TETRIS ARCADE
/**************************************\
 *
 * Made By: Logan Bowers and Mark Sabbagh
 * For: 	AP CSA Period 5
 * 			Cmdr. Schenk
 * 			Spring, 2019
 * 			Submitted May 3, 2019
 * 
/**************************************\

*/

package bowers_sabbagh;

/*
 * A Note about imports:
 * This project uses the processing core library in order to render graphics. This library
 * is open source and provides most of the visual functionality required for this project.
 * 
 * In addition, this project also relies on the processing io library to get GPIO inputs
 * from the Raspberry Pi and the minim library for sound management.
 */

//Imports required to use processing
import processing.core.*;
import processing.data.*;

import processing.io.*; //Library with which to interact with the GPIO pins

import ddf.minim.*; //Library used to manage sound

//Java libraries (in alphabetical order)
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The application in which the program runs
 * <p>
 * This class extends the PApplet class provided by the processing core engine
 * and is additionally the insertion point for the main method. All event
 * handling for the application must be done in this class
 * <p>
 * Unless explicitly specified in the method description, it is assumed that
 * whoever is listed as the author of a class also wrote the methods in that
 * class.
 * 
 * @author Mark Sabbagh
 * @author Logan Bowers
 * @version 1.0
 */
public class TetrisArcade extends PApplet {

	private static final int width = 320; // Width of the application window on startup
	private static final int height = 480; // Height of the application window on startup

	GameManager gameManager; // Instantiation of the GameManager class used to control the game (this should
								// only be instantiated once)

	/**
	 * Method that is called before setup in order to set the size of the screen and
	 * the graphics context
	 */
	public void settings() {
		size(width, height, P2D);
	}

	/**
	 * This method is called once when the application starts. The game manager is
	 * instantiated in this method and the GPIO input pins are set up to receive
	 * input from a controller.
	 */
	public void setup() {

		gameManager = new GameManager(this, false); // Instantiate the game manager

		gameManager.startGame(0); // start the game at level 0 when the application loads
	}

	/**
	 * This method runs on every frame after setup. The run method essentially
	 * transfers control of the game to the game manager from this point on.
	 * 
	 * @see GameManager#run()
	 */
	public void draw() {
		gameManager.run();
	}

	// Events

	/** Key is pressed on the keyboard */
	public void keyPressed() {
		gameManager.inputManager.setPressInput();
	}

	/** Key is released from the keyboard */
	public void keyReleased() {
		gameManager.inputManager.setReleaseInput();
	}

	/**
	 * Button is pressed or released on the controller
	 * 
	 * @param pin The id of the GPIO pin that triggered the event
	 */
	public void buttonEvent(int pin) {
		if (GPIO.digitalRead(pin) == GPIO.LOW) {
			gameManager.inputManager.setControllerInput(pin, true);
		} else {
			gameManager.inputManager.setControllerInput(pin, false);
		}
	}

	/****************************************************************/

	/**
	 * A class that handles the algorithm that the AI uses while playing when the game has been inactive
	 * for a period of time
	 * @author Mark Sabbagh
	 */
	public class AI {

		// Height, lines, holes, roughness
		private double[] weights = { 0.510066f, 0.760666f, 0.35663f, 0.184483f }; // The weights for how strongly the
																					// different parameters effect the
																					// outcome of the algorithm
		/**
		 * Default Constructor
		 */
		public AI() {
		}

		/**
		 * Uses the current grid state and the weights associated with the calculated height, lines, holes and roughness
		 * to determine the best response to the situation
		 * @param gridManager The grid that the AI is using to play the game
		 * @return A float containing the AI's output position and orientation calculated from the weights and the grid inputs
		 */
		private float[] getAIResponse(GridManager gridManager) {
			double bestScore = Double.MIN_VALUE;
			float[] moves = { -1, -1 };

			// Loop through every situation and find best score
			for (int r = 0; r < 4; r++) {
				for (int p = 0; p < 10; p++) {
					// Clone original GridManager
					GridManager tempGridManager = gridManager.clone();

					// Rotate current piece
					for (int i = 0; i < r; i++)
						tempGridManager.rotateCurrentPiece(true, false);

					// Move to the very left
					for (int i = 0; i < 5; i++)
						tempGridManager.moveCurrentPiece(Direction.LEFT, false);

					// Move it to pos
					for (int i = 0; i < p; i++)
						tempGridManager.moveCurrentPiece(Direction.RIGHT, false);

					// Fall until collision
					boolean collided = false;

					while (!collided) {
						collided = tempGridManager.moveCurrentPiece(Direction.DOWN, false);
					}

					// Add blocks to grid
					Block[] pieceBlocks = tempGridManager.currentPiece.getBlocks().clone();
					for (int z = 0; z < pieceBlocks.length; z++) {
						int x = pieceBlocks[z].getX();
						int y = pieceBlocks[z].getY();
						if (y >= 0)
							tempGridManager.blocks[x][y] = pieceBlocks[z].clone();
					}

					// Calculate score
					double score = -(weights[0] * calcTotalHieght(tempGridManager.blocks))
							+ (weights[1] * calcLines(tempGridManager.blocks))
							- (weights[2] * calcHoles(tempGridManager.blocks))
							- (weights[3] * calcRoughness(tempGridManager.blocks));

					// Update moves if found better score
					if (score > bestScore) {
						bestScore = score;
						moves[0] = tempGridManager.currentPiece.x;
						moves[1] = tempGridManager.currentPiece.orientation;
					}
				}
			}

			return moves;
		}

		/**
		 * Calculates the total height by summing the heights of each column in the grid
		 * @param grid The blocks within the grid
		 * @return The height input to use in the response algorithm
		 */
		private int calcTotalHieght(Block[][] grid) {
			int total = 0;
			for (int x = 0; x < grid.length; x++) {
				total += getColHeight(grid, x, false);
			}
			return total;
		}

		/**
		 * Calculates the number of lines that would be cleared in the grid
		 * @param grid The blocks within the grid
		 * @return The lines input to use in the response algorithm
		 */
		private int calcLines(Block[][] grid) {
			int lines = 0;

			for (int y = 0; y < grid[0].length; y++) {
				int counter = 0;
				for (int x = 0; x < grid.length; x++) {
					if (grid[x][y] != null)
						counter++;
				}

				if (counter == 10)
					lines++;
			}

			return lines;
		}

		/**
		 * Calculates the number of holes in the grid
		 * @param grid The blocks within the grid
		 * @return The holes input to use in the response algorithm
		 */
		private int calcHoles(Block[][] grid) {

			int holes = 0;

			for (int x = 0; x < grid.length; x++) {
				int yStart = getColHeight(grid, x, true) + 1;
				for (int y = yStart; y < grid[0].length; y++) {
					if (grid[x][y] == null)
						holes++;
				}
			}

			return holes;
		}

		/**
		 * Calculates how "rough" the grid is by summing the differences in heights between
		 * adjacent columns
		 * @param grid The blocks within the grid
		 * @return The roughness input to use in the response algorithm
		 */
		private double calcRoughness(Block[][] grid) {
			double total = 0;

			for (int x = 0; x < grid.length - 1; x++) {
				total += Math.abs(getColHeight(grid, x, false) - getColHeight(grid, x + 1, false));
			}

			return total;
		}

		/**
		 * @param grid The blocks within the grid
		 * @param col The column to get the height of
		 * @param returnGridHeight If <code>true</code>, the height is measured from the top, otherwise it is measured from the bottom
		 * @return The height of the column
		 */
		private int getColHeight(Block[][] grid, int col, boolean returnGridHeight) {
			for (int y = 0; y < grid[0].length; y++) {
				if (grid[col][y] != null) {
					if (returnGridHeight)
						return y;
					else
						return grid[0].length - y;
				}
			}
			if (returnGridHeight)
				return 24;
			else
				return 0;
		}

		/**
		 * Passes the outputs given by the AI response to the current game grid
		 * so that it can handle the inputs required to move the piece to that position and orientation
		 * @param GridManager The grid manager to pass the outputs to
		 */
		private void passToGrid(GridManager GridManager) {
			float[] aiOutput = getAIResponse(GridManager.clone());
			GridManager.AITargetPos = aiOutput[0];
			GridManager.AITargetRot = round(aiOutput[1]);
		}
	}

	/****************************************************************/

	/**
	 * This class represents a single displayable block on the game grid. The block
	 * class includes standard display variables such as position, size, color,
	 * etc., and is also linked to a specific piece within the game.
	 * 
	 * @author Mark Sabbagh
	 * @author Logan Bowers
	 */
	public class Block implements IDisplay {

		private int[] blockColor; // An array holding the colors of the block when displayed normally and in
									// intense mode
		private int x; // The x position of this block, given in block units from the top left of the
						// grid
		private int y; // The y position of this block, given in block units from the top left of the
						// grid
		private float size; // The pixel size of the block (also determines scaling for xand y positions)
		private float intenseSize; // The pixel size of the block while in intense mode
		private PieceType pieceType; // The piece type that was used to instantiate this block

		/**
		 * Default constructor. This constructor is not recommended for practical use.
		 */
		public Block() {
			this.x = 0;
			this.y = 0;
			this.blockColor = new int[2];
			this.size = 10;
			this.intenseSize = 9;
			this.pieceType = PieceType.NULL;
		}

		/**
		 * @param x          The x position of this block, given in block units from the
		 *                   top left of the grid
		 * @param y          The y position of this block, given in block units from the
		 *                   top left of the grid
		 * @param size       The pixel size of the block (also determines scaling for x
		 *                   and y positions)
		 * @param blockColor The color of the block when displayed normally and in
		 *                   intense mode
		 * @param pieceType  The piece type that was used to instantiate this block
		 */
		public Block(int x, int y, float size, int[] blockColor, PieceType pieceType) {
			this.x = x;
			this.y = y;
			this.blockColor = blockColor;
			this.size = size;
			this.intenseSize = size * .9f;
			this.pieceType = pieceType;
		}

		/*
		 * (non-Javadoc) Creates a new block object with the same fields
		 * 
		 * @see java.lang.Object#clone()
		 */
		public Block clone() {
			return new Block(this.x, this.y, this.size, this.blockColor, this.pieceType);
		}

		/*
		 * (non-Javadoc) Displays the block onto the screen
		 * 
		 * @see bowers_sabbagh.TetrisArcade.IDisplay#display(boolean)
		 */
		@Override
		public void display(boolean intenseMode) {
			rectMode(CORNER);
			if (intenseMode) {
				noStroke();
				fill(45);
				rect(x * size, y * size, intenseSize, intenseSize);
				if (intenseSize < size * .9f)
					intenseSize++;
			} else {
				fill(blockColor[0]);
				// stroke(180);
				// strokeWeight(2);
				noStroke();
				ellipseMode(CORNER);
				rect(x * size, y * size, size - 2, size - 2);
				fill(0, 50);
				rect(x * size, y * size, size - 6, size - 6);
				ellipseMode(CENTER);
			}
		}

		/**
		 * @return An array holding the colors of the block when displayed normally and
		 *         in intense mode
		 */
		public int[] getColor() {
			return this.blockColor;
		}

		/**
		 * @param blockColor An array holding the colors of the block when displayed
		 *                   normally and in intense mode
		 */
		public void setColor(int[] blockColor) {
			this.blockColor = blockColor;
		}

		/**
		 * @param x The x position of this block, given in block units from the top left
		 *          of the grid
		 * @param y The y position of this block, given in block units from the top left
		 *          of the grid
		 */
		public void setPos(int x, int y) {
			this.x = x;
			this.y = y;
		}

		/**
		 * @return The x position of this block, given in block units from the top left
		 *         of the grid
		 */
		public int getX() {
			return this.x;
		}

		/**
		 * @param x The x position of this block, given in block units from the top left
		 *          of the grid
		 */
		public void setX(int x) {
			this.x = x;
		}

		/**
		 * @return The y position of this block, given in block units from the top left
		 *         of the grid
		 */
		public int getY() {
			return this.y;
		}

		/**
		 * @param y The y position of this block, given in block units from the top left
		 *          of the grid
		 */
		public void setY(int y) {
			this.y = y;
		}

		/**
		 * @return The pixel size of the block
		 */
		public float getSize() {
			return size;
		}

		/**
		 * @param size The pixel size of the block (also determines scaling for x and y
		 *             positions)
		 */
		public void setSize(float size) {
			this.size = size;
		}

		/**
		 * @return The pixel size of the block while in intense mode
		 */
		public float getIntenseSize() {
			return intenseSize;
		}

		/**
		 * @param intenseSize The pixel size of the block while in intense mode
		 */
		public void setIntenseSize(float intenseSize) {
			this.intenseSize = intenseSize;
		}

		/**
		 * @return The piece type that was used to instantiate this block
		 */
		public PieceType getPieceType() {
			return this.pieceType;
		}
	}

	/****************************************************************/

	/**
	 * A class that can be displayed as an interactive button. This class contains
	 * various graphics settings such as borders and colors for when the button is
	 * active or not, and can be displayed to the screen using the IDisplay
	 * interface.
	 * 
	 * @author Logan Bowers
	 */
	public class Button implements IDisplay {

		// fields
		private float x; // The x position of the button from the top left of the screen in pixels
		private float y; // The y position of the button from the top left of the screen in pixels
		private float w; // The pixel width of the button
		private float h; // The pixel height of the button
		private int color; // The color of the button when inactive

		private boolean selected; // Whether the button is active
		private int selectedColor; // The color of the button when active

		/**
		 * Default constructor. This constructor is not recommended for practical use.
		 */
		public Button() {
			// default values
			this.x = 0;
			this.y = 0;
			this.w = 15;
			this.h = 10;
			this.color = 255;
			this.selected = false;
			this.selectedColor = 0xff89ffff;
		}

		/**
		 * Constructs this object using x, y, width, height, and color
		 * 
		 * @param x     The x position of the button from the top left of the screen in
		 *              pixels
		 * @param y     The y position of the button from the top left of the screen in
		 *              pixels
		 * @param w     The pixel width of the button
		 * @param h     The pixel height of the button
		 * @param color The color of the button when inactive
		 */
		public Button(float x, float y, float w, float h, int color) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.color = color;
			this.selected = false;
			this.selectedColor = 0xffdddddd;
		}

		/**
		 * @param x             The x position of the button from the top left of the
		 *                      screen in pixels
		 * @param y             The y position of the button from the top left of the
		 *                      screen in pixels
		 * @param w             The pixel width of the button
		 * @param h             The pixel height of the button
		 * @param color         The color of the button when inactive
		 * @param selected      Whether the button is active
		 * @param selectedColor The color of the button when active
		 */
		public Button(float x, float y, float w, float h, int color, boolean selected, int selectedColor) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.color = color;
			this.selected = selected;
			this.selectedColor = selectedColor;
		}

		/*
		 * (non-Javadoc) Displays the button according to the buttons fields
		 * 
		 * @see bowers_sabbagh.TetrisArcade.IDisplay#display(boolean)
		 */
		public void display(boolean intenseMode) {

			noStroke();
			if (selected) {
				fill(this.selectedColor);
			} else {
				fill(this.color);
			}

			rectMode(CENTER);
			rect(this.x, this.y, this.w, this.h);
		}

		/**
		 * @return The x position of the button from the top left of the screen in
		 *         pixels
		 */
		public float getX() {
			return x;
		}

		/**
		 * @param x The x position of the button from the top left of the screen in
		 *          pixels
		 */
		public void setX(float x) {
			this.x = x;
		}

		/**
		 * @return The y position of the button from the top left of the screen in
		 *         pixels
		 */
		public float getY() {
			return y;
		}

		/**
		 * @param y The y position of the button from the top left of the screen in
		 *          pixels
		 */
		public void setY(float y) {
			this.y = y;
		}

		/**
		 * @return The pixel width of the button
		 */
		public float getW() {
			return w;
		}

		/**
		 * @param w The pixel width of the button
		 */
		public void setW(float w) {
			this.w = w;
		}

		/**
		 * @return The pixel height of the button
		 */
		public float getH() {
			return h;
		}

		/**
		 * @param h The pixel height of the button
		 */
		public void setH(float h) {
			this.h = h;
		}

		/**
		 * @return The color of the button when inactive
		 */
		public int getColor() {
			return color;
		}

		/**
		 * @param color The color of the button when inactive
		 */
		public void setColor(int color) {
			this.color = color;
		}

		/**
		 * @return The color of the button when active
		 */
		public int getSelectedColor() {
			return selectedColor;
		}

		/**
		 * @param selectedColor The color of the button when active
		 */
		public void setSelectedColor(int selectedColor) {
			this.selectedColor = selectedColor;
		}

		/**
		 * @return Whether the button is active
		 */
		public boolean isSelected() {
			return selected;
		}

		/**
		 * @param selected Whether the button is active
		 */
		public void setSelected(boolean selected) {
			this.selected = selected;
		}
	}

	/**
	 * An enum describing a geographic direction that is not necessarily dependent
	 * on any input.
	 * <p>
	 * If the direction is not known, the NULL option should be used in place of a
	 * direction.
	 * 
	 * @author Logan Bowers
	 */
	public enum Direction {
		DOWN(0), LEFT(1), RIGHT(2), UP(3), NULL(4);

		private int directionIndex; // The number associated with the direction

		/**
		 * Constructs a Direction enum value (private because enums are only created
		 * internally)
		 * 
		 * @param index The number associated with the direction
		 */
		private Direction(int index) {
			this.directionIndex = index;
		}

		/**
		 * @param index The index of the desired direction
		 * @return A direction, assuming a direction was found with the given index
		 * @throws IllegalArgumentException if no direction is found with the given
		 *                                  index
		 */
		public static Direction get(int index) {
			for (Direction l : Direction.values()) {
				if (l.directionIndex == index)
					return l;
			}
			throw new IllegalArgumentException("Direction not found");
		}

		/**
		 * @return The index associated with the direction that called this method
		 */
		public int getIndex() {
			return directionIndex;
		}
	}

	/****************************************************************/

	/**
	 * This is an abstract class used to create a visual effect on the screen while
	 * in the game. Each effect has two crucial methods: run(), which updates fields
	 * established by the childs of this class, and display(boolean), which is
	 * inherited from the display interface and draws the specific effect frame onto
	 * the screen.
	 * 
	 * @author Mark Sabbagh
	 */
	public abstract class Effect implements IDisplay {

		protected float x; // The x position where the effect occurs
		protected float y; // The y position where the effect occurs
		protected int lifeTime; // The number of frames during which this effect occurs
		protected boolean onlyRunInIntense; // Whether the effect should only run in intense mode

		/**
		 * Default Constructor. Do not use this constructor.
		 */
		public Effect() {
			this.x = 0;
			this.y = 0;
			this.lifeTime = 0;
			this.onlyRunInIntense = false;
		}

		/**
		 * @param x                The x position where the effect occurs
		 * @param y                The y position where the effect occurs
		 * @param lifeTime         The number of frames during which this effect takes
		 *                         place
		 * @param onlyRunInIntense Whether the effect should only run in intense mode
		 */
		public Effect(float x, float y, int lifeTime, boolean onlyRunInIntense) {
			this.x = x;
			this.y = y;
			this.lifeTime = lifeTime;
			this.onlyRunInIntense = onlyRunInIntense;
		}

		/**
		 * This method should be run on every frame that the effect is being used and
		 * should update fields accordingly. It should also decrement the lifetime
		 * counter for every time that it is called
		 */
		public abstract void run();

		// READ_WRITE

		/**
		 * @return The x position where the effect occurs
		 */
		public float getX() {
			return x;
		}

		/**
		 * @param x The x position where the effect occurs
		 */
		public void setX(float x) {
			this.x = x;
		}

		/**
		 * @return The y position where the effect occurs
		 */
		public float getY() {
			return y;
		}

		/**
		 * @param y The y position where the effect occurs
		 */
		public void setY(float y) {
			this.y = y;
		}

		// READ_ONLY

		/**
		 * @return The number of frames during which this effect occurs
		 */
		public int getLifeTime() {
			return this.lifeTime;
		}

		/**
		 * @return Whether the effect should only run in intense mode
		 */
		public boolean canOnlyRunIntense() {
			return this.onlyRunInIntense;
		}
	}

	/****************************************************************/

	/**
	 * Creates an outline of the current piece, which is then projected outward from
	 * the piece when it collides, creating an explosion effect.
	 * 
	 * @author Mark Sabbagh
	 */
	public class ExplodeEffect extends Effect {
		private float size; // The size of the outline as it explodes outward from the piece

		/**
		 * Default Constructor. Do not use this constructor.
		 */
		public ExplodeEffect() {
			super(0, 0, 0, false);
			this.size = 0;
		}

		/**
		 * @param x                The x position where the effect takes place
		 * @param y                The y position where the effect takes place
		 * @param lifeTime         The number of frames during which this effect takes
		 *                         place
		 * @param onlyRunInIntense Whether the effect should only run in intense mode
		 * @param size             The size of the outline as it explodes outward from
		 *                         the piece
		 */
		public ExplodeEffect(float x, float y, int lifeTime, boolean runInIntense, float size) {
			super(x, y, lifeTime, runInIntense);
			this.size = size * 2;
		}

		/*
		 * (non-Javadoc) Decrements the lifetime and increases the size field
		 * 
		 * @see bowers_sabbagh.TetrisArcade.Effect#run()
		 */
		public void run() {
			lifeTime--;
			size *= 1.2f;
		}

		/*
		 * (non-Javadoc) Displays the effect according to its size
		 * 
		 * @see bowers_sabbagh.TetrisArcade.IDisplay#display(boolean)
		 */
		@Override
		public void display(boolean intenseMode) {
			if (intenseMode) {
				strokeWeight(4);
				stroke(255, 255 - size * 1.4f);
				noFill();
				rectMode(CENTER);
				rect(x, y, size, size);
			} else {
				println("Only runs in intense mode");
			}
		}

		/**
		 * @return The size of the outline as it explodes outward from the piece
		 */
		public float getSize() {
			return size;
		}

		/**
		 * @param size The size of the outline as it explodes outward from the piece
		 */
		public void setSize(float size) {
			this.size = size;
		}
	}

	/****************************************************************/

	/**
	 * This class handles all of the game logic, which includes managing game state
	 * and handling events. The methods in this class provide ways for controlling
	 * the player and in game data.
	 * 
	 * @author Mark Sabbagh
	 * @author Logan Bowers
	 */
	public class GameManager implements IDisplay {

		/*
		 * Fields
		 */

		// player data
		private String name; // The name of the player
		private String oldName; // The name of the player that is saved when the game is paused (used for
								// menuing)
		private int score; // The current score of the player
		private Table highScores; // All of the high scores recorded by the game
		private Table displayedScores; // The scores that are displayed on the score board

		// game data
		private int startLevel; // The level that the player chose to start at
		private int oldStartLevel; // The level that the player previously chose to start at when the game was
									// paused
		private int level; // The level that the game is currently on
		private int countDown; // The time left before the mode changes
		private int startDelay; // The number of frames between the call to startGame and when the game actually
								// starts
		private int inactiveTimer; // The number of frames that the player has been inactive
		private boolean intenseMode; // Whether the game is in intense mode
		private boolean inMenu; // Whether the menu screen is being displayed

		// misc settings
		private PFont font; // The font to use for text
		private boolean usingController; // Whether a controller is being used

		// managers (basically subclasses of the game manager)
		private InputManager inputManager; // Handles input from a controller or keyboard
		private GridManager gridManager; // Handles the logic of the in game grid
		private MenuManager menuManager; // Handles the logic of the pause menu
		private SoundManager soundManager; // Handles all music and sound effects

		private AI ai; // The ai that plays when the game is inactive

		// Cheat Codes
		private boolean jeff; // Activates JEFF mode if true, which includes extra sound effects
		private boolean alwaysIntense; // Forces the game to always run in intense
		private boolean madeTop10; // If the player's score is currently in the top 10
		private boolean madeTop2; // If the player's score is currently in the top 2

		/**
		 * Default Constructor
		 * 
		 * @param application     The processing application that instantiated this
		 *                        object
		 * @param usingController Whether the player is using a controller or not
		 *                        (should be true when used on the PI)
		 */
		public GameManager(PApplet application, boolean usingController) {

			this.name = "PLAYER";
			this.oldName = "PLAYER";
			this.score = 0;
			this.highScores = this.loadScores();
			this.displayedScores = this.getScoresNoDuplicates();

			this.startLevel = 0;
			this.oldStartLevel = 0;
			this.level = 0;
			this.countDown = 20 * 60;
			this.startDelay = 20;
			this.inactiveTimer = 1200;
			this.intenseMode = false;
			this.inMenu = false;

			this.font = createFont("tetrisFont.ttf", 64);
			this.usingController = usingController;
			if (this.usingController) {
				this.setupController(application);
			}

			this.inputManager = new InputManager();
			this.menuManager = new MenuManager(this);
			this.soundManager = new SoundManager(new Minim(application));
			this.gridManager = new GridManager(this);

			this.ai = new AI();

			this.jeff = false;
			this.alwaysIntense = false;
			this.madeTop10 = false;
			this.madeTop2 = false;
		}

		/**
		 * This method sets up the GPIO pins and should be called if the player is using
		 * a controller.
		 * 
		 * @param application The application passed to the GameManager constructor
		 */
		private void setupController(PApplet application) {
			int[] pins = gameManager.inputManager.inputPins; // get the GPIO pins that correlate to controller inputs
			for (int i = 0; i < pins.length; i++) { // Each pin
				GPIO.pinMode(pins[i], GPIO.INPUT_PULLUP); // Set the pins to receive input (using pullup resistor)
				GPIO.attachInterrupt(pins[i], application, "buttonEvent", GPIO.CHANGE); // Set the event handler for
																						// button input
			}
		}

		// Game State Handlers

		/**
		 * This is the main loop for the entire game and should be called every frame.
		 */
		public void run() {

			// Handle Inputs
			if (usingController) {
				inputManager.checkControllerInputs();
			}
			inputManager.updateInputTriggers();
			this.evaluateInputs();

			// Only update the game when not in the menu
			if (!this.inMenu) {
				this.updateGame();
				if (inactiveTimer == 0) {
					// The AI plays the game
					this.gridManager.moveToAITarget();
					this.name = "AI";
				}
			}

			// Display everything
			gridManager.display(intenseMode);
			this.display(intenseMode);

			// Decrement the timer
			if (inactiveTimer > 0)
				inactiveTimer--;
		}

		/**
		 * Initializes all values before the game starts and handles cheat codes
		 * (triggers associated with the names)
		 * 
		 * @param startLevel The level that the player starts at
		 */
		public void startGame(int startLevel) {
			// handling cheat codes
			switch (this.name) {
			case "JEFF":
				this.jeff = true;
				this.madeTop10 = false;
				this.madeTop2 = false;
				break;
			case "JOSEPH":
				this.startLevel = 15;
				break;
			case "INTENSE":
				this.alwaysIntense = true;
				break;
			case "LOGAN":
			case "MARK":
				this.alwaysIntense = true;
				this.startLevel = 16;
				break;
			default:
				this.jeff = false;
				this.alwaysIntense = false;
				this.startLevel = startLevel;
				break;
			}

			// Deleting names if the player's name is DEL + some number
			if (this.name.startsWith("DEL")) {
				for (int i = 0; i < 10; i++) {
					if (this.name.equalsIgnoreCase("DEL" + i)) {
						deleteScoreByName(this.displayedScores.getRow(i).getString("name"), i);
					}
				}
			}

			// Setting intial values for the start of the game
			this.score = 0;
			this.level = this.startLevel;
			this.gridManager = new GridManager(this);
			this.countDown = this.soundManager.gameMusic.length();

			this.startDelay = 20;

			if (this.alwaysIntense) {
				this.intenseMode = true;
				soundManager.startMusicFromLoop();
			} else {
				this.intenseMode = false;
				soundManager.startMusicFromBeginning();
			}

			this.oldName = this.name;
		}

		/**
		 * Updates fields while the player is playing the game (not in the pause menu)
		 */
		public void updateGame() {
			// Handle transitions between modes (including music)
			if (!(countDown > 0)) {
				if (this.alwaysIntense) {
					this.soundManager.gameMusic
							.skip(soundManager.transitionPoints[0] - this.soundManager.gameMusic.position());
				} else {
					changeMode();
				}
			}

			// The countdown that is displayed in the display function is determined based
			// on the state of the music
			this.countDown = soundManager.transitionPoints[soundManager.currentMusic]
					- soundManager.gameMusic.position();

			// update the grid
			if (startDelay > 0) {
				startDelay--;
			} else {
				gridManager.update();
			}

			if (intenseMode)
				shakeScreen();
		}

		/**
		 * Saves the player's score and resets the game if it is in progress
		 */
		public void reset() {

			if (inactiveTimer > 0) {
				inactiveTimer = 1200;
			}

			soundManager.rewindMusic();
			if (score > 0)
				saveScore();
			startGame(this.startLevel);
		}

		// Score handling

		/**
		 * Loads the high scores from persistent storage
		 * <p>
		 * Written by Logan Bowers
		 * 
		 * @return A table of all high scores stored in the file
		 */
		private Table loadScores() {
			File temp = new File(dataPath("scores.csv"));
			Table scores;
			// initializing the high score table
			if (temp.exists()) {
				scores = loadTable("scores.csv", "header, csv");
			} else {
				// create new table
				scores = new Table();
				scores.addColumn("name");
				scores.addColumn("score");
				saveTable(this.highScores, "data/scores.csv", "csv");
			}

			return scores;
		}

		/**
		 * Saves a score in the high scores table and sorts it accordingly (this is
		 * usually called after the player gets a high score
		 * <p>
		 * Written by Logan Bowers
		 */
		private void saveScore() {

			TableRow newRow = this.highScores.addRow();
			newRow.setString("name", this.name);
			newRow.setInt("score", this.score);

			// sort new score by insertion
			if (this.score > 0) {
				int i = this.highScores.getRowCount() - 2; // second to last row
				while ((i >= 0) && (this.highScores.getRow(i + 1).getInt("score") > this.highScores.getRow(i)
						.getInt("score"))) {
					TableRow swappedRow1 = this.highScores.getRow(i);
					TableRow swappedRow2 = this.highScores.getRow(i + 1);
					String tempName = swappedRow1.getString("name");
					int tempScore = swappedRow1.getInt("score");
					swappedRow1.setString("name", swappedRow2.getString("name"));
					swappedRow1.setInt("score", swappedRow2.getInt("score"));
					swappedRow2.setString("name", tempName);
					swappedRow2.setInt("score", tempScore);
					i--;
				}
			}

			saveTable(this.highScores, "data/scores.csv", "csv");
			this.displayedScores = this.getScoresNoDuplicates();
		}

		/**
		 * Creates a table that takes only the best score from one name
		 * <p>
		 * Written by Logan Bowers
		 * 
		 * @return A table consisting of the high score table without any duplicate
		 *         names
		 */
		public Table getScoresNoDuplicates() {
			// remove duplicate names and create a new table with only the unique names
			Table noDuplicates = new Table();
			noDuplicates.addColumn("name");
			noDuplicates.addColumn("score");
			ArrayList<String> names = new ArrayList<String>();
			for (TableRow row : this.highScores.rows()) {
				if (!names.contains(row.getString("name"))) {
					noDuplicates.addRow(row);
					names.add(row.getString("name"));
				}
			}

			return noDuplicates;
		}

		/**
		 * Deletes the score associated with the name and position on the score board
		 * from the displayed scores and the overall data table
		 * <p>
		 * Written by Logan Bowers
		 * 
		 * @param name     Name to delete
		 * @param rowIndex The position of the name on the score board
		 */
		private void deleteScoreByName(String name, int rowIndex) {
			// Delete name from high scores
			for (int j = 0; j < this.highScores.getRowCount(); j++) {
				if (this.highScores.getRow(j).getString("name").equals(name)) {
					this.highScores.removeRow(j);
				}
			}

			// delete name from displayed scores
			this.displayedScores.removeRow(rowIndex);
		}

		// Input handling

		/**
		 * Handles all of the input triggers that are active when this is called Note:
		 * this method is called multiple times when a key is pressed down, so the calls
		 * do not correspond to the traditional processing key events
		 * <p>
		 * Written by Mark Sabbagh
		 */
		private void evaluateInputs() {
			for (int i = 0; i < inputManager.triggers.length; i++) {
				if (inputManager.triggers[i] == true) {
					this.executeFunction(InputType.get(i));
				}
			}
		}

		/**
		 * Handles the routine associated with the input given
		 * <p>
		 * Written by Mark Sabbagh
		 * 
		 * @param input The input given by the player
		 */
		private void executeFunction(InputType input) {
			if (inMenu) {
				// transfer control to the menu manager
				menuManager.executeFunction(input);
			} else {
				// The player is in the game
				switch (input) {

				case LEFT:
					gridManager.moveCurrentPiece(Direction.LEFT, true);
					inputManager.inputs[1] = false;
					inputManager.triggers[1] = false;
					break;

				case RIGHT:
					gridManager.moveCurrentPiece(Direction.RIGHT, true);
					inputManager.inputs[0] = false;
					inputManager.triggers[0] = false;
					break;

				case UP:
					soundManager.gameMusic.pause();
					this.oldStartLevel = this.startLevel;
					this.oldName = name;
					inputManager.changeDelays(true);
					inMenu ^= true;
					break;

				case DOWN:
					gridManager.moveCurrentPiece(Direction.DOWN, true);
					break;

				case A:
					gridManager.rotateCurrentPiece(false, false);

					break;

				case B:
					gridManager.rotateCurrentPiece(true, false);
					break;

				default:
					break;
				}

				// Stops the AI from playing and returns to the menu if the player has been
				// inactive
				if (inactiveTimer == 0) {
					reset();
					soundManager.gameMusic.pause();
					this.oldStartLevel = this.startLevel;
					this.oldName = name;
					inMenu = true;
				}
			}

			inactiveTimer = 1200;
		}

		/**
		 * Skips the normal delay for the specified input (used for when the player
		 * presses an input into a wall)
		 * 
		 * @param input The input that should be activated
		 */
		public void chargeInput(InputType input) {
			inputManager.timers[input.getIndex()] = 0;
		}

		// Misc. methods

		/**
		 * Shakes the screen for a time specified by the grid manager
		 * <p>
		 * Written by Mark Sabbagh
		 */
		public void shakeScreen() {
			if (gridManager.shakeTimer > 0) {
				translate(random(-width / 50, width / 50), random(-height / 50, height / 50));
				gridManager.shakeTimer--;
			} else {
				translate(0, 0);
			}
		}

		/**
		 * Changes the game mode from normal to intense and vise versa
		 * <p>
		 * Written by Mark Sabbagh
		 */
		public void changeMode() {
			intenseMode ^= true;
			if (intenseMode) {
				// change music
				if (soundManager.currentMusic == 2)
					soundManager.gameMusic.skip(soundManager.transitionPoints[0] - soundManager.transitionPoints[2]);
				soundManager.currentMusic = 1;

				// change font
				this.font = createFont("modernFont.ttf", 64);

				// make blocks fall faster
				gridManager.setFallRate(gridManager.levelIntenseSpeeds[level]);
			} else {
				// change music
				soundManager.currentMusic = 2;

				// change font
				this.font = createFont("tetrisFont.ttf", 64);

				// return blocks to normal speed
				gridManager.setFallRate(gridManager.levelSpeeds[level]);
			}
		}

		/**
		 * Plays a random sound clip from a collection if the game is in jeff mode
		 * <p>
		 * Written by Logan Bowers
		 */
		public void handleTetrisForJeff() {
			if (this.jeff) {
				if (this.gridManager.lastDrought >= 15) {
					soundManager.playSoundEffect("IntenseBoom", false);
				} else {
					Random rng = new Random();
					int x = rng.nextInt(5);
					soundManager.playSoundEffect("BoomTetrisForJeff" + x, false);
				}
			}
		}

		/*
		 * (non-Javadoc) Displays the player stats for the game and also displays the
		 * high score board if the AI is playing the game
		 * 
		 * @see bowers_sabbagh.TetrisArcade.IDisplay#display(boolean)
		 */
		@Override
		public void display(boolean intenseMode) {
			textFont(this.font);
			textAlign(CENTER, CENTER);

			pushMatrix();

			// Setup position of UI
			if (intenseMode) {
				fill(0);
				stroke(0);
			} else {
				fill(this.gridManager.currentPiece.pieceColor[0]);
				stroke(this.gridManager.currentPiece.pieceColor[0]);
			}

			textSize(width * .09f);
			strokeWeight(2);

			line(this.gridManager.getGridEndPos(), 0, this.gridManager.getGridEndPos(), height);
			translate(this.gridManager.getGridEndPos() + (width - this.gridManager.getGridEndPos()) / 2, 0);

			// Draw UI text
			text("TETRIS", 0, height * .05f);

			textSize(width * .08f);
			text("score\n" + score, 0, height * .35f);

			text("level\n" + level, 0, height * .55f);

			text("lines\n" + this.gridManager.getTotalLinesCleared(), 0, height * 0.75f);

			textSize(width * .13f);
			text(this.countDown / 1000, 0, height * .9f);
			popMatrix();

			if (this.inMenu) {
				this.menuManager.display(this.intenseMode);
			} else if (this.inactiveTimer == 0) {
				// Display High Scores with AI playing

				noStroke();
				fill(0, 160);
				rectMode(CENTER);
				rect(width / 2, height / 2, width * 1.2f, height * 1.2f);

				if (intenseMode) {
					fill(255);
				} else {
					fill(this.gridManager.currentPiece.pieceColor[0]);
				}

				float ySpace = height * .04f;
				textSize(ySpace);

				for (int i = 0; i < Math.min(this.displayedScores.getRowCount(), 10); i++) {
					TableRow row = this.displayedScores.getRow(i);
					text(row.getString("name") + " " + row.getInt("score"), 100, height * .1f + (ySpace * i));
				}
			}
		}

		// READ_WRITE

		/**
		 * @return The name of the player
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name The name of the player
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return The name of the player that is saved when the game is paused
		 */
		public String getOldName() {
			return oldName;
		}

		/**
		 * @param oldName The name of the player that is saved when the game is paused
		 */
		public void setOldName(String oldName) {
			this.oldName = oldName;
		}

		/**
		 * @return The current score of the player
		 */
		public int getScore() {
			return score;
		}

		/**
		 * @param score The current score of the player
		 */
		public void setScore(int score) {
			this.score = score;
		}

		/**
		 * @return The level that the player chose to start at
		 */
		public int getStartLevel() {
			return startLevel;
		}

		/**
		 * @param startLevel The level that the player chose to start at
		 */
		public void setStartLevel(int startLevel) {
			this.startLevel = startLevel;
		}

		/**
		 * @return The level that the game is currently on
		 */
		public int getLevel() {
			return level;
		}

		/**
		 * @param level The level that the game is currently on
		 */
		public void setLevel(int level) {
			this.level = level;
		}

		/**
		 * @return The level that the player previously chose to start at when the game
		 *         was paused
		 */
		public int getOldStartLevel() {
			return oldStartLevel;
		}

		/**
		 * @param oldStartLevel The level that the player previously chose to start at
		 *                      when the game was paused
		 */
		public void setOldStartLevel(int oldStartLevel) {
			this.oldStartLevel = oldStartLevel;
		}

		/**
		 * @return The number of frames between the call to startGame and when the game
		 *         actually starts
		 */
		public int getStartDelay() {
			return startDelay;
		}

		/**
		 * @param startDelay The number of frames between the call to startGame and when
		 *                   the game actually starts
		 */
		public void setStartDelay(int startDelay) {
			this.startDelay = startDelay;
		}

		/**
		 * @return Whether the game is in intense mode
		 */
		public boolean isIntenseMode() {
			return intenseMode;
		}

		/**
		 * @param intenseMode Whether the game is in intense mode
		 */
		public void setIntenseMode(boolean intenseMode) {
			this.intenseMode = intenseMode;
		}

		/**
		 * @return Whether the menu screen is being displayed
		 */
		public boolean isInMenu() {
			return inMenu;
		}

		/**
		 * @param inMenu Whether the menu screen is being displayed
		 */
		public void setInMenu(boolean inMenu) {
			this.inMenu = inMenu;
		}

		/**
		 * @return The font to use for text
		 */
		public PFont getFont() {
			return font;
		}

		/**
		 * @param font The font to use for text
		 */
		public void setFont(PFont font) {
			this.font = font;
		}

		/**
		 * @return Whether a controller is being used
		 */
		public boolean isUsingController() {
			return usingController;
		}

		/**
		 * @param usingController Whether a controller is being used
		 */
		public void setUsingController(boolean usingController) {
			this.usingController = usingController;
		}

		/**
		 * @return if Jeff mode is on
		 */
		public boolean isJeff() {
			return jeff;
		}

		/**
		 * @param jeff Activates JEFF mode if true, which includes extra sound effects
		 */
		public void setJeff(boolean jeff) {
			this.jeff = jeff;
		}

		/**
		 * @return Forces the game to always run in intense
		 */
		public boolean isAlwaysIntense() {
			return alwaysIntense;
		}

		/**
		 * @param alwaysIntense Forces the game to always run in intense
		 */
		public void setAlwaysIntense(boolean alwaysIntense) {
			this.alwaysIntense = alwaysIntense;
		}

		/**
		 * @return If the player's score is currently in the top 10
		 */
		public boolean madeTop10() {
			return madeTop10;
		}

		/**
		 * @param madeTop10 If the player's score is currently in the top 10
		 */
		public void setMadeTop10(boolean madeTop10) {
			this.madeTop10 = madeTop10;
		}

		/**
		 * @return If the player's score is currently in the top 2
		 */
		public boolean madeTop2() {
			return madeTop2;
		}

		/**
		 * @param madeTop2 If the player's score is currently in the top 2
		 */
		public void setMadeTop2(boolean madeTop2) {
			this.madeTop2 = madeTop2;
		}

		// READ_ONLY

		/**
		 * @return All of the high scores recorded by the game
		 */
		public Table getHighScores() {
			return this.highScores;
		}

		/**
		 * @return The scores that are displayed on the score board
		 */
		public Table getDisplayedScores() {
			return displayedScores;
		}

		/**
		 * @return The time left before the mode changes
		 */
		public int getCountDown() {
			return countDown;
		}

		/**
		 * @return The number of frames that the player has been inactive
		 */
		public int getInactiveTimer() {
			return inactiveTimer;
		}

		/**
		 * @return Manager that handles input from a controller or keyboard
		 */
		public InputManager getInputManager() {
			return inputManager;
		}

		/**
		 * @return Manager that handles the logic of the in game grid
		 */
		public GridManager getGridManager() {
			return gridManager;
		}

		/**
		 * @return Manager that handles the logic of the pause menu
		 */
		public MenuManager getMenuManager() {
			return menuManager;
		}

		/**
		 * @return Manager that handles all music and sound effects
		 */
		public SoundManager getSoundManager() {
			return soundManager;
		}

		/**
		 * @return The ai that plays when the game is inactive
		 */
		public AI getAi() {
			return ai;
		}
	}

	/****************************************************************/

	/**
	 * This class handles everything (logic, colors, effects, etc.) associated with
	 * the game grid. The handling of any piece movement and logic associated with
	 * collisions and line clears are also in this class. As such, this is the
	 * longest class in the whole program. A single instance of this class should
	 * always exist within the Game Manager while the game is running.
	 * 
	 * @author Mark Sabbagh
	 * @author Logan Bowers
	 */
	public class GridManager implements IDisplay {

		/*
		 * Constants
		 */

		// General settings
		private final int pieceSpawnX; // The x position that the piece spawns in
		private final float nextBoxX; // The block x position of the next box
		private final float nextBoxY; // The block y position of the next box
		private final int w; // The block width of the grid
		private final int h; // The block height of the grid
		private final int linesToLevelUp; // The number of lines required to level up
		private final float levelFactor; // The amount that the scoring increases by for each level up
		private final int intenseColor; // The color of the pieces in intense mode
		private final int pieceSpawnDelay; // The number of frames that occur between the collision of the last piece
											// and the spawning of the next piece

		// level speeds
		private final int[] levelSpeeds = { 45, 40, 35, 30, 26, 22, 18, 14, 11, 8, 6, 5, 4, 3, 2, 1, 0 }; // The fall
																											// rate of
																											// each
																											// level
		private final int[] levelIntenseSpeeds = { 35, 30, 25, 22, 18, 15, 13, 11, 8, 6, 5, 4, 3, 2, 1, 0, 0 }; // The
																												// fall
																												// rate
																												// of
																												// each
																												// level
																												// in
																												// intense
																												// mode

		// Color Schemes
		private final int[][] colorScheme = { { 0xfff44141, 0xfff4a941, 0xfff4e541, 0, 35 },
				{ 0xffa3f441, 0xff4192f4, 0xff42f4c2, 100, 140 }, { 0xff9141f4, 0xfff441d3, 0xfff44167, 190, 240 },
				{ 0xffeff2f7, 0xffc43131, 0xffffa530, 5, 20 }, { 0xffaff441, 0xffebf441, 0xfff49b42, 20, 50 } };

		/*
		 * Fields
		 */

		private GameManager gameManager; // A reference to the game

		// grid fields
		private Block[][] blocks; // A 10 * 24 array of blocks that are contained within the grid
		private float spacing; // The size of the blocks
		private float gridEndPos; // The x position where the grid stops

		private Piece currentPiece; // The current piece displayed on the grid
		private Piece nextPiece; // The piece displayed in the next box
		private float AITargetPos; // The piece x position on the grid that the AI chooses
		private int AITargetRot; // The piece orientation that the AI chooses

		// score/line related fields
		private int totalLinesCleared; // The total number of lines cleared since the game started
		private ArrayList<Integer> linesToClear; // The lines that still need to be cleared
		private int drought; // The number of pieces that have passed since the last I piece
		private int lastDrought; // The previous value of drought before the last I piece spawned

		// timers
		private int fallRate; // The rate at which pieces fall from the top
		private int fallTimer; // The number of frames before the current piece falls another block
		private int nextPieceTimer; // The number of frames before the next piece will spawn
		private int shakeTimer; // The number of frames that the grid will shake in intense mode
		private float graphicsTimer; // A timer that controls the background color during intense mode
		private int streakEffectTimer; // The number of frames that the streak effect lasts for

		private ArrayList<Effect> effects; // A list of effects that are currently taking place in the game

		/**
		 * @param gameManager A reference to the game
		 */
		public GridManager(GameManager gameManager) {

			// Setting constants
			this.pieceSpawnX = 5;
			this.nextBoxX = 12.5f;
			this.nextBoxY = 4;
			this.w = 10;
			this.h = 24;
			this.linesToLevelUp = 5;
			this.levelFactor = 0.8f;
			this.intenseColor = 45;
			this.pieceSpawnDelay = 10;

			// setting fields
			this.gameManager = gameManager;

			this.blocks = new Block[10][24];
			this.spacing = height / 24f;
			this.gridEndPos = this.spacing * this.blocks.length;

			this.currentPiece = generateRandomPiece();
			this.nextPiece = generateRandomPiece();
			this.nextPiece.setPos(nextBoxX, nextBoxY);
			this.AITargetPos = 0;
			this.AITargetRot = 0;

			this.totalLinesCleared = 0;
			this.linesToClear = new ArrayList<Integer>();
			this.drought = 0;
			this.lastDrought = 0;

			this.fallRate = this.levelSpeeds[gameManager.getLevel()];
			this.fallTimer = fallRate;
			this.nextPieceTimer = 0;
			this.shakeTimer = 0;
			this.graphicsTimer = 0;
			this.streakEffectTimer = 0;

			this.effects = new ArrayList<Effect>();
		}

		/*
		 * (non-Javadoc) Creates an exact duplicate of the grid manager with the same
		 * fields as the original
		 * 
		 * @see java.lang.Object#clone()
		 */
		public GridManager clone() {
			GridManager tempGridManager = new GridManager(gameManager);

			for (int i = 0; i < this.blocks.length; i++) {
				for (int j = 0; j < this.blocks[1].length; j++) {
					if (this.blocks[i][j] != null) {
						tempGridManager.blocks[i][j] = this.blocks[i][j].clone();
					}
				}
			}

			tempGridManager.currentPiece = this.currentPiece.clone();

			return tempGridManager;
		}

		/**
		 * Moves the current piece according to the AI's response (only when the AI is
		 * playing)
		 * <p>
		 * Written by Mark Sabbagh
		 */
		public void moveToAITarget() {
			if (currentPiece.orientation != AITargetRot)
				rotateCurrentPiece(true, false);
			else {
				if (currentPiece.x < AITargetPos)
					moveCurrentPiece(Direction.RIGHT, true);
				else if (currentPiece.x > AITargetPos)
					moveCurrentPiece(Direction.LEFT, true);
				else
					moveCurrentPiece(Direction.DOWN, true);
			}
		}

		/**
		 * Updates the pieces in the grid and any effects associated with the grid.
		 */
		public void update() {

			// move piece
			if (!currentPiece.isLocked()) {
				if (fallTimer > 0) {
					fallTimer--;
				} else {
					moveCurrentPiece(Direction.DOWN, true);
					fallTimer = fallRate;
				}
			} else if (nextPieceTimer > 0) {
				// Waiting for next piece to spawn
				nextPieceTimer--;
			} else {
				if (linesToClear.size() > 0) {
					clearLines();
				}
				pushPiece();
			}

			// Running all effects
			for (int i = effects.size() - 1; i >= 0; i--) {
				Effect E = effects.get(i);
				if (E.getLifeTime() <= 0)
					effects.remove(i);
				else {
					if (E.canOnlyRunIntense()) {
						if (gameManager.isIntenseMode())
							E.run();
						else
							effects.remove(i);
					} else {
						E.run();
					}
				}
			}
		}

		// Piece functionality methods

		/**
		 * Puts the next piece into the current piece position and generates a new next
		 * piece
		 * <p>
		 * Written by Logan Bowers
		 */
		public void pushPiece() {

			this.currentPiece = this.nextPiece;
			this.currentPiece.setPos(pieceSpawnX, getPieceSpawnY(this.currentPiece.getType()));

			// Checking if the player lost
			for (Block b : this.currentPiece.getBlocks()) {
				if (this.blocks[b.getX()][b.getY()] != null) {
					// Jeff Mode
					if (gameManager.jeff) {
						gameManager.soundManager.playSoundEffect("Lost", false);
					}

					gameManager.reset();
					return;
				}
			}

			this.nextPiece = this.generateRandomPiece();
			this.nextPiece.setPos(nextBoxX, nextBoxY);

			if (gameManager.inactiveTimer == 0) {
				gameManager.ai.passToGrid(this);
			}

			// Handle Droughts
			if (this.currentPiece.type == PieceType.I) {
				this.lastDrought = this.drought;
				this.drought = 0;
			} else {
				this.drought++;
			}

			// Jeff Mode
			if (gameManager.jeff) {
				if ((this.drought == 15) && (this.currentPiece.type != PieceType.I)) {
					gameManager.soundManager.playSoundEffect("Drought", false);
				}

				if ((this.lastDrought >= 15) && (this.currentPiece.type == PieceType.I)) {
					gameManager.soundManager.playSoundEffect("LongBar", false);
				}
			}

		}

		/**
		 * Uses rng to generate a piece with a random piece type
		 * 
		 * @return A piece with random piece type
		 */
		public Piece generateRandomPiece() {

			Random rng = new Random(); // Creating a random number generator
			int x = rng.nextInt(7);

			float y = getPieceSpawnY(PieceType.get(x));
			Piece piece = new Piece(pieceSpawnX, y, PieceType.get(x), this.spacing, getColorOfPiece(PieceType.get(x)),
					false); // Create a piece with random piece type

			return piece;
		}

		/**
		 * Checks if the current piece can be moved in the direction specified, then
		 * moves the piece if no collision is detected. If a collision is detected, it
		 * is handled appropriately
		 * 
		 * @param dir              The direction that the player (or AI) is attempting
		 *                         to move the piece
		 * @param handleCollisions Whether the method should handle collisions if a
		 *                         collision was detected
		 * @return <code>true</code> if the piece collided
		 */
		private boolean moveCurrentPiece(Direction dir, boolean handleCollisions) {
			switch (dir) {
			case DOWN:
				if (!checkCollisions(dir)) {
					currentPiece.setY(currentPiece.getY() + 1);
					return false;
				} else {
					// Only handle collisions on the bottom of the piece
					if (handleCollisions) {
						handleCollision();
					}
				}
				return true;
			case LEFT:
				if (!checkCollisions(dir)) {
					currentPiece.setX(currentPiece.getX() - 1);
					return false;
				} else {
					gameManager.chargeInput(InputType.LEFT);
					return true;
				}
			case RIGHT:
				if (!checkCollisions(dir)) {
					currentPiece.setX(currentPiece.getX() + 1);
					return false;
				} else {
					gameManager.chargeInput(InputType.RIGHT);
					return true;
				}
			default:
				break;
			}
			currentPiece.updateBlocks();
			return false;
		}

		/**
		 * Checks if the current piece can be rotated in the direction specified, then
		 * rotates the piece if no rotation collision is detected.
		 * 
		 * @param cw        <code>true</code> if the piece is rotating clockwise.
		 *                  <code>false</code> if counterclockwise
		 * @param dontCheck <code>true</code> if the method should not check for
		 *                  rotation collisions
		 */
		public void rotateCurrentPiece(boolean cw, boolean dontCheck) {
			switch (currentPiece.type) {
			case O:
				break;

			case S:
			case Z:
			case I:
				if (currentPiece.orientation == 1) {
					if (dontCheck || !checkRotationCollision(false))
						currentPiece.rotate(false);
				} else if (dontCheck || !checkRotationCollision(true))
					currentPiece.rotate(true);
				break;

			case L:
			case J:
			case T:
				if (dontCheck || !checkRotationCollision(cw))
					currentPiece.rotate(cw);
				break;
			default:
				break;
			}
		}

		// Calculation methods

		/**
		 * Checks for potential collisions between walls or blocks if the current piece
		 * were to move in the direction specified
		 * 
		 * @param dir The direction that the player (or AI) is attempting to move the
		 *            piece
		 * @return <code>true</code> if a collision was detected
		 */
		private boolean checkCollisions(Direction dir) {
			if (!currentPiece.isLocked()) {
				Block[] pieceBlocks = new Block[currentPiece.getBlocks().length];

				// Clone piece blocks and move the piece by 1 unit in the direction specified by
				// dir
				for (int i = 0; i < pieceBlocks.length; i++) {
					pieceBlocks[i] = currentPiece.getBlocks()[i].clone();
					switch (dir) {
					case DOWN:
						pieceBlocks[i].setY(pieceBlocks[i].getY() + 1);
						break;
					case LEFT:
						pieceBlocks[i].setX(pieceBlocks[i].getX() - 1);
						break;
					case RIGHT:
						pieceBlocks[i].setX(pieceBlocks[i].getX() + 1);
						break;
					default:
						break;
					}
				}

				// Checking the new piece position for if it is out of bounds or inside another
				// block

				// Iterating through piece blocks
				for (int i = 0; i < pieceBlocks.length; i++) {
					if (pieceBlocks[i].y >= h) {
						// Collision with bottom
						return true;
					} else if ((pieceBlocks[i].x >= w) || (pieceBlocks[i].x < 0)) {
						// Collision with walls
						return true;
					} else {
						// Need to check every block in the grid for a collision
						for (int x = 0; x < this.blocks.length; x++) {
							for (int y = 0; y < this.blocks[0].length; y++) {
								if (this.blocks[x][y] != null) {
									Block b = this.blocks[x][y].clone();
									if ((b.x == pieceBlocks[i].x) && (b.y == pieceBlocks[i].y)) {
										// Collision with another block
										return true;
									}
								}
							}
						}
					}
				}

				return false;
			} else {
				return true;
			}
		}

		/**
		 * Checks for potential collisions if the current piece were to be rotated in
		 * the direction specified
		 * 
		 * @param cw <code>true</code> if the piece is rotating clockwise.
		 *           <code>false</code> if counterclockwise
		 * @return <code>true</code> if a collision was detected
		 */
		public boolean checkRotationCollision(boolean cw) {
			Block[] pieceBlocks = new Block[currentPiece.getBlocks().length];

			rotateCurrentPiece(cw, true);

			for (int i = 0; i < pieceBlocks.length; i++) {
				pieceBlocks[i] = currentPiece.getBlocks()[i].clone();
			}

			rotateCurrentPiece(!cw, true);

			for (int i = 0; i < pieceBlocks.length; i++) {
				if (pieceBlocks[i].y >= h || pieceBlocks[i].x >= w || pieceBlocks[i].x < 0) {
					return true;
				} else {
					for (int x = 0; x < this.blocks.length; x++) {
						for (int y = 0; y < this.blocks[0].length; y++) {
							if (this.blocks[x][y] != null) {
								Block b = this.blocks[x][y].clone();
								if ((b.x == pieceBlocks[i].x) && (b.y == pieceBlocks[i].y))
									return true;
							}
						}
					}
				}
			}
			return false;
		}

		/**
		 * This method executes whenever a collision is detected on a piece that moves
		 * down. The method adds the current piece blocks to the grid before the piece
		 * is destroyed, checks for line clears, and waits for the next piece to spawn.
		 */
		private void handleCollision() {
			Block[] pieceBlocks = this.currentPiece.getBlocks().clone();

			for (int z = 0; z < pieceBlocks.length; z++) {
				int x = pieceBlocks[z].getX();
				int y = pieceBlocks[z].getY();
				if (y >= 0)
					this.blocks[x][y] = pieceBlocks[z].clone();
				else
					gameManager.reset();
			}

			checkLines();

			effects.add(new ExplodeEffect(this.currentPiece.getX() * spacing, this.currentPiece.getY() * spacing, 10,
					true, spacing));
			shakeTimer = 8;

			this.currentPiece.locked = true;
			nextPieceTimer = pieceSpawnDelay;
		}

		/**
		 * Finds the number of lines that need to be cleared and adds them to a field so
		 * that they can be cleared in update.
		 */
		private void checkLines() {
			this.linesToClear = new ArrayList<Integer>();

			// Find the number of lines that need to be cleared
			for (int y = 0; y < this.blocks[0].length; y++) {
				int counter = 0; // Number of squares that are filled

				for (int x = 0; x < this.blocks.length; x++) {
					if (this.blocks[x][y] != null)
						counter++;
				}

				if (counter == w) { // All squares are filled
					effects.add(new LineClearEffect(0, (y) * spacing, 10, false, spacing));
					this.linesToClear.add(y);
				}
			}

			// Handle sound and visual effects
			if (linesToClear.size() == 0) {
				gameManager.soundManager.playSoundEffect("Drop", gameManager.intenseMode);
			} else if (linesToClear.size() == 4) {
				// add tetris visual effect
				float avgY = 0;

				for (int i = 0; i < 4; i++) {
					avgY += linesToClear.get(i);
				}

				avgY /= 4;

				effects.add(new LineExplodeEffect((spacing * 10) / 2, avgY * spacing, 200, true, 13));

				// play tetris sound effect
				gameManager.soundManager.playSoundEffect("Tetris", gameManager.intenseMode);
			} else {
				gameManager.soundManager.playSoundEffect("Clear", gameManager.intenseMode);
			}
		}

		/**
		 * Clears the lines that were found using checkLines(). This method handles the
		 * changes in score and level that are associated with line clears.
		 * 
		 * @see #clearLine(int)
		 */
		private void clearLines() {
			for (int i = 0; i < this.linesToClear.size(); i++) {
				clearLine(this.linesToClear.get(i));
			}

			// Add to score
			float factor = 1 + levelFactor * gameManager.getLevel();
			if (linesToClear.size() == 4) {
				gameManager.setScore(round(gameManager.getScore() + (1000 * factor)));

				gameManager.handleTetrisForJeff();

				linesToClear = new ArrayList<Integer>();
			} else if (linesToClear.size() == 3) {
				gameManager.setScore(round(gameManager.getScore() + (600 * factor)));
				linesToClear = new ArrayList<Integer>();
			} else if (linesToClear.size() == 2) {
				gameManager.setScore(round(gameManager.getScore() + (300 * factor)));
				linesToClear = new ArrayList<Integer>();
			} else {
				gameManager.setScore(round(gameManager.getScore() + (100 * factor)));
				linesToClear = new ArrayList<Integer>();
			}

			// Add to level
			if ((totalLinesCleared >= linesToLevelUp * (gameManager.getLevel() + 1)) && gameManager.level != 16) {
				levelUp();
			}

			// Jeff mode
			if (gameManager.jeff) {
				int top10Score = gameManager.displayedScores.getInt(gameManager.displayedScores.getRowCount() - 1,
						"score");
				int top2Score = 0;
				if (gameManager.displayedScores.getRowCount() >= 2) {
					top2Score = gameManager.displayedScores.getInt(1, "score");
				}

				if ((!gameManager.madeTop10) && (gameManager.score > top10Score)) {
					gameManager.soundManager.playSoundEffect("NeckandNeck", false);
					gameManager.madeTop10 = true;
				}

				if ((!gameManager.madeTop2) && (gameManager.score > top2Score)) {
					gameManager.soundManager.playSoundEffect("Top2", false);
					gameManager.madeTop2 = true;
				}
			}

		}

		/**
		 * Clears a single line and adds it to the running total of lines cleared
		 * 
		 * @param line The y position of the line to clear
		 */
		private void clearLine(int line) {

			for (int y = line - 1; y >= 0; y--) {
				for (int x = 0; x < w; x++) {
					Block b = this.blocks[x][y];
					if (b != null)
						this.blocks[x][y].setY(this.blocks[x][y].getY() + 1);

					this.blocks[x][y + 1] = this.blocks[x][y];
				}
			}
			totalLinesCleared++;
		}

		/**
		 * Increases the level and handles differences in color and speed
		 */
		private void levelUp() {

			gameManager.setLevel(gameManager.getLevel() + 1);

			// update block colors
			for (Block[] bArray : this.blocks) {
				for (Block b : bArray) {
					if (b != null) {
						b.setColor(getColorOfPiece(b.getPieceType()));
					}
				}
			}

			// update next piece color
			this.nextPiece.setPieceColor(getColorOfPiece(this.nextPiece.getType()));

			// change fall rate
			if (gameManager.isIntenseMode())
				fallRate = levelIntenseSpeeds[Math.min(gameManager.getLevel(), levelSpeeds.length - 1)];
			else
				fallRate = levelSpeeds[Math.min(gameManager.getLevel(), levelSpeeds.length - 1)];
		}

		/**
		 * Gets the color of a piece by using the color schemes of the current level
		 * 
		 * @param type The piece type to get the color of
		 * @return An array holding the colors of the piece in normal and intense mode
		 */
		private int[] getColorOfPiece(PieceType type) {
			int[] c = new int[2];
			switch (type) {
			case I:
			case O:
			case T:
				c[0] = colorScheme[gameManager.getLevel() % 5][0];
				break;
			case L:
			case Z:
				c[0] = colorScheme[gameManager.getLevel() % 5][1];
				break;
			case J:
			case S:
				c[0] = colorScheme[gameManager.getLevel() % 5][2];
				break;

			default:
				break;
			}
			c[1] = intenseColor;

			return c;
		}

		/**
		 * Finds the y spawn point of a piece given its piece type
		 * 
		 * @param p The piece type to get the spawn point of
		 * @return The y coordinate of the spawn point of this piece type
		 */
		public float getPieceSpawnY(PieceType p) {
			float y;
			switch (p) {
			case S:
			case Z:
			case O:
				y = 0.5f;
			default:
				y = 0;
				return y;
			}
		}

		/*
		 * (non-Javadoc) Displays everything inside of the game grid
		 * 
		 * @see bowers_sabbagh.TetrisArcade.IDisplay#display(boolean)
		 */
		@Override
		public void display(boolean intenseMode) {

			if (!intenseMode) {
				background(0);
				// image(gridImage, 0, 0);
				graphicsTimer = 0;
			} else {
				colorMode(HSB);
				background(map(sin(graphicsTimer), -1, 1, colorScheme[gameManager.getLevel() % 5][3],
						colorScheme[gameManager.getLevel() % 5][4]), 255, 255);
				graphicsTimer += .03f;
			}

			// Display blocks
			for (int i = 0; i < this.blocks.length; i++) {
				for (int j = 0; j < this.blocks[1].length; j++) {
					if (this.blocks[i][j] != null) {
						this.blocks[i][j].display(intenseMode);
					}
				}
			}

			nextPiece.display(intenseMode);
			currentPiece.display(intenseMode);

			if (intenseMode) {
				if (streakEffectTimer > 0) {
					streakEffectTimer--;
				} else {
					streakEffectTimer = 7;
					effects.add(new StreakEffect(random(0, spacing * 10), height * 1.4f, 200, true, 20));
				}
			}

			for (Effect E : effects) {
				if (E.canOnlyRunIntense()) {
					if (intenseMode)
						E.display(intenseMode);
				} else {
					E.display(intenseMode);
				}
			}
		}

		// READ_WRITE

		/**
		 * @return The size of the blocks
		 */
		public float getSpacing() {
			return spacing;
		}

		/**
		 * @param spacing The size of the blocks
		 */
		public void setSpacing(float spacing) {
			this.spacing = spacing;
		}

		/**
		 * @return The x position where the grid stops
		 */
		public float getGridEndPos() {
			return gridEndPos;
		}

		/**
		 * @param gridEndPos The x position where the grid stops
		 */
		public void setGridEndPos(float gridManagerEndPos) {
			gridEndPos = gridManagerEndPos;
		}

		/**
		 * @return The current piece displayed on the grid
		 */
		public Piece getCurrentPiece() {
			return currentPiece;
		}

		/**
		 * @param currentPiece The current piece displayed on the grid
		 */
		public void setCurrentPiece(Piece currentPiece) {
			this.currentPiece = currentPiece;
		}

		/**
		 * @return The piece displayed in the next box
		 */
		public Piece getNextPiece() {
			return nextPiece;
		}

		/**
		 * @param nextPiece The piece displayed in the next box
		 */
		public void setNextPiece(Piece nextPiece) {
			this.nextPiece = nextPiece;
		}

		/**
		 * @return The piece x position on the grid that the AI chooses
		 */
		public float getAITargetPos() {
			return AITargetPos;
		}

		/**
		 * @param aITargetPos The piece x position on the grid that the AI chooses
		 */
		public void setAITargetPos(float aITargetPos) {
			AITargetPos = aITargetPos;
		}

		/**
		 * @return The piece orientation that the AI chooses
		 */
		public int getAITargetRot() {
			return AITargetRot;
		}

		/**
		 * @param aITargetRot The piece orientation that the AI chooses
		 */
		public void setAITargetRot(int aITargetRot) {
			AITargetRot = aITargetRot;
		}

		/**
		 * @return The rate at which pieces fall from the top
		 */
		public int getFallRate() {
			return fallRate;
		}

		/**
		 * @param fallRate The rate at which pieces fall from the top
		 */
		public void setFallRate(int fallRate) {
			this.fallRate = fallRate;
		}

		// READ_ONLY

		/**
		 * @return The x position that the piece spawns in
		 */
		public int getPieceSpawnX() {
			return pieceSpawnX;
		}

		/**
		 * @return The block x position of the next box
		 */
		public float getNextBoxX() {
			return nextBoxX;
		}

		/**
		 * @return The block y position of the next box
		 */
		public float getNextBoxY() {
			return nextBoxY;
		}

		/**
		 * @return The block width of the grid
		 */
		public int getW() {
			return w;
		}

		/**
		 * @return The block height of the grid
		 */
		public int getH() {
			return h;
		}

		/**
		 * @return The number of lines required to level up
		 */
		public int getLinesToLevelUp() {
			return linesToLevelUp;
		}

		/**
		 * @return The amount that the scoring increases by for each level up
		 */
		public float getLevelFactor() {
			return levelFactor;
		}

		/**
		 * @return The color of the pieces in intense mode
		 */
		public int getIntenseColor() {
			return intenseColor;
		}

		/**
		 * @return The number of frames that occur between the collision of the last
		 *         piece and the spawning of the next piece
		 */
		public int getPieceSpawnDelay() {
			return pieceSpawnDelay;
		}

		/**
		 * @return The fall rate of each level
		 */
		public int[] getLevelSpeeds() {
			return levelSpeeds;
		}

		/**
		 * @return The fall rate of each level in intense mode
		 */
		public int[] getLevelIntenseSpeeds() {
			return levelIntenseSpeeds;
		}

		/**
		 * @return The color scheme for each level
		 */
		public int[][] getColorScheme() {
			return colorScheme;
		}

		/**
		 * @return A reference to the game
		 */
		public GameManager getGameManager() {
			return gameManager;
		}

		/**
		 * @return A 10 * 24 array of blocks that are contained within the grid
		 */
		public Block[][] getBlocks() {
			return blocks;
		}

		/**
		 * @return The total number of lines cleared since the game started
		 */
		public int getTotalLinesCleared() {
			return totalLinesCleared;
		}

		/**
		 * @return The lines that still need to be cleared
		 */
		public ArrayList<Integer> getLinesToClear() {
			return linesToClear;
		}

		/**
		 * @return The number of pieces that have passed since the last I piece
		 */
		public int getDrought() {
			return drought;
		}

		/**
		 * @return The previous value of drought before the last I piece spawned
		 */
		public int getLastDrought() {
			return lastDrought;
		}

		/**
		 * @return The number of frames before the current piece falls another block
		 */
		public int getFallTimer() {
			return fallTimer;
		}

		/**
		 * @return The number of frames before the next piece will spawn
		 */
		public int getNextPieceTimer() {
			return nextPieceTimer;
		}

		/**
		 * @return The number of frames that the grid will shake in intense mode
		 */
		public int getShakeTimer() {
			return shakeTimer;
		}

		/**
		 * @return A timer that controls the background color during intense mode
		 */
		public float getGraphicsTimer() {
			return graphicsTimer;
		}

		/**
		 * @return The number of frames that the streak effect lasts for
		 */
		public int getStreakEffectTimer() {
			return streakEffectTimer;
		}

		/**
		 * @return A list of effects that are currently taking place in the game
		 */
		public ArrayList<Effect> getEffects() {
			return effects;
		}

	}

	/****************************************************************/

	/**
	 * An interface for displaying graphics to the screen. Classes that implement
	 * this interface should be linked to the processing graphics library and be
	 * able to draw to the application
	 * 
	 * @author Mark Sabbagh
	 */
	public interface IDisplay {
		/**
		 * Draws the contents of the object that implements this method to the
		 * processing application. Different graphics can be used depending on if the
		 * game is in intense mode or not.
		 * 
		 * @param intenseMode Whether the game is in intense mode
		 */
		public void display(boolean intenseMode);
	}

	/****************************************************************/

	/**
	 * An ini file used to hold data/configuration settings by using key value
	 * pairs. Ini files are organized by sections starting with [ and ending with ].
	 * For example: [my section]. Key value pairs are denoted "key = value".
	 * <p>
	 * In this class, values can be accessed as a string, int, float, or double.
	 * <p>
	 * This class utilizes several classes from java's included libraries (see
	 * annotations).
	 * 
	 * @author Aerospace (stack overflow user)
	 * @author Logan Bowers
	 * @see java.io.BufferedReader
	 * @see java.io.FileReader
	 * @see java.util.Map
	 * @see java.util.regex.Matcher
	 * @see java.util.regex.Pattern
	 */

	public class IniFile {

		// These patterns are used by the matcher to search for specific general regex
		// expressions

		/** Regex expression used to find a section header in the file */
		private Pattern _section = Pattern.compile("\\s*\\[([^]]*)\\]\\s*");
		/** Regex expression used to find a key value pair in the file */
		private Pattern _keyValue = Pattern.compile("\\s*([^=]*)=(.*)");
		/** Entries that are described by a section and a key, value pair */
		private Map<String, Map<String, String>> _entries = new HashMap<String, Map<String, String>>();

		/**
		 * Creates a new Ini File object with the specified path. Note that this
		 * constructor automatically parses the .ini file specified by the path and thus
		 * leaves the _entries field populated.
		 * 
		 * @param path The path (absolute or relative to the project directory) to the
		 *             ini file.
		 * @throws IOException thrown if no config file can be found at the specified
		 *                     path
		 * @see #parse(String)
		 */
		public IniFile(String path) throws IOException {
			parse(path);
		}

		/**
		 * Parses the Ini File with the specified path. Specifically, this method is
		 * used to populate the _entries Map with key-value pairs that describe the ini
		 * file specified by the given path.
		 * 
		 * @param path The path (absolute or relative to the project directory) to the
		 *             ini file that is to be parsed.
		 * @throws IOException thrown if no config file can be found at the specified
		 *                     path
		 */
		public void parse(String path) throws IOException {

			// Buffered Reader is a resource, which is why it is called with the try
			// statement
			try (BufferedReader br = new BufferedReader(new FileReader(path));) {
				// Strings used to hold information within the file
				String line;
				String section = null;

				/*
				 * This while loop executes for every line in the file detected by the buffered
				 * reader. Each line is matched against one of the patterns declared at the top
				 * of the class, which is then used to create a mapping entry in _entries.
				 */
				while ((line = br.readLine()) != null) { // While a line exists to read

					Matcher m = _section.matcher(line); // Testing the line in the file against the _section pattern

					if (m.matches()) { // Line is a section
						section = m.group(1).trim();
					} else if (section != null) { // The file has a section (in other words: this method is useless if
													// the
													// .ini file does not have a header)

						m = _keyValue.matcher(line);

						if (m.matches()) { // Line is a key/value pair
							String k = m.group(1).trim(); // Get the key
							String value = m.group(2).trim(); // Get the value

							// Create the key value mapping
							Map<String, String> kv = _entries.get(section); // Index into the map (outer layer is
																			// <section,
																			// kv>)
							if (kv == null) { // Only create a new section if it has not already been created.
								_entries.put(section, kv = new HashMap<String, String>());
							}
							kv.put(k, value); // Put the new key value pair into the mapping specified by the section
						}
					}
				}
			} // End of try statement. Buffered reader is discarded.
		}

		/**
		 * Gets a string value from the specified section and key.
		 * 
		 * @param section      The section in which the key is located
		 * @param key          The key to retrieve the value from.
		 * @param defaultvalue The default value to return should the section and key
		 *                     specified not be found
		 * @return Either:
		 *         <p>
		 *         1. the value given by the section and key parameters or
		 *         <p>
		 *         2. The default value given as a parameter if no such value could be
		 *         found
		 */
		public String getString(String section, String key, String defaultvalue) {
			Map<String, String> kv = _entries.get(section);
			if (kv == null) {
				return defaultvalue;
			}
			return kv.get(key);
		}

		/**
		 * Gets an int value from the specified section and key.
		 * 
		 * @param section      The section in which the key is located
		 * @param key          The key to retrieve the value from.
		 * @param defaultvalue The default value to return should the section and key
		 *                     specified not be found
		 * @return Either:
		 *         <p>
		 *         1. the value given by the section and key parameters or
		 *         <p>
		 *         2. The default value given as a parameter if no such value could be
		 *         found
		 */
		public int getInt(String section, String key, int defaultvalue) {
			Map<String, String> kv = _entries.get(section);
			if (kv == null) {
				return defaultvalue;
			} else if (kv.get(key).startsWith("#")) { // Converting hexadecimal values from the file into something
														// readable
														// by the compiler
				String value = kv.get(key);
				return Integer.parseInt(value.substring(1), 16);
			}
			return Integer.parseInt(kv.get(key));
		}

		/**
		 * Gets a float value from the specified section and key.
		 * 
		 * @param section      The section in which the key is located
		 * @param key          The key to retrieve the value from.
		 * @param defaultvalue The default value to return should the section and key
		 *                     specified not be found
		 * @return Either:
		 *         <p>
		 *         1. the value given by the section and key parameters or
		 *         <p>
		 *         2. The default value given as a parameter if no such value could be
		 *         found
		 */
		public float getFloat(String section, String key, float defaultvalue) {
			Map<String, String> kv = _entries.get(section);
			if (kv == null) {
				return defaultvalue;
			}
			return Float.parseFloat(kv.get(key));
		}

		/**
		 * Gets a double value from the specified section and key.
		 * 
		 * @param section      The section in which the key is located
		 * @param key          The key to retrieve the value from.
		 * @param defaultvalue The default value to return should the section and key
		 *                     specified not be found
		 * @return Either:
		 *         <p>
		 *         1. the value given by the section and key parameters or
		 *         <p>
		 *         2. The default value given as a parameter if no such value could be
		 *         found
		 */
		public double getDouble(String section, String key, double defaultvalue) {
			Map<String, String> kv = _entries.get(section);
			if (kv == null) {
				return defaultvalue;
			}
			return Double.parseDouble(kv.get(key));
		}
	}

	/****************************************************************/

	/**
	 * Class that handles input from a keyboard or controller. This class implements
	 * a trigger system so that inputs can be continually triggered when a key is
	 * held down.
	 * 
	 * @author Mark Sabbagh
	 * @author Logan Bowers
	 */
	public class InputManager {

		/*
		 * Constants
		 */

		private final int[] keyboardInputs = { 'a', 'd', 'w', 's', 'j', 'k' }; // The ASCII values for the keyboard
																				// inputs
		private final int[] inputPins = { 26, 19, 16, 20, 21, 13 }; // Pin ids for the controller inputs

		/*
		 * Fields
		 */
		private int[] delayTimer = { 13, 13, 18, 0, 18, 18 }; // # of frames between the input and the second trigger
																// (the first is immediate)
		private int[] startInputTimer = { 4, 4, 4, 2, 12, 12 }; // # of frames between every successive trigger after
																// the second trigger
		private boolean[] inputs; // The inputs that are currently being pressed
		private boolean[] triggers; // The triggers that are active (the corresponding timer must expire in order
									// for these to be active)
		private int[] timers; // The # of frames before the next input corresponding to the timer

		/**
		 * Default Constructor : sets all of the inputs and triggers to false and sets
		 * the timers to 0
		 */
		public InputManager() {

			// Setting fields
			this.inputs = new boolean[6];
			this.triggers = new boolean[6];
			this.timers = new int[6];
			for (int i = 0; i < inputs.length; i++) {
				inputs[i] = false;
				triggers[i] = false;
				timers[i] = 0;
			}
		}

		/**
		 * Sets any of the triggers for which the timer has expired to true so that an
		 * input event can be triggered. All other triggers are set to false and the
		 * timer is reset.
		 */
		public void updateInputTriggers() {
			for (int i = 0; i < inputs.length; i++) {
				if (inputs[i]) {
					if (timers[i] == delayTimer[i]) {
						triggers[i] = true;
						timers[i]--;
					} else if (timers[i] > 0) {
						timers[i]--;
						triggers[i] = false;
					} else {
						timers[i] = startInputTimer[i];
						triggers[i] = true;
					}
				}
			}
		}

		/**
		 * This method changes the delay timers for when the user is in the menu vs the
		 * game
		 * 
		 * @param inMenu If the player is in the menu
		 */
		public void changeDelays(boolean inMenu) {
			if (inMenu) {
				this.delayTimer[3] = 18;
				this.startInputTimer[3] = 4;
			} else {
				this.delayTimer[3] = 0;
				this.startInputTimer[3] = 2;
			}
		}

		/**
		 * This method is called by any controller event (press or release) and
		 * processes the event by updating the inputs and triggers.
		 * 
		 * @param pin     The pin id of the button pressed
		 * @param pressed <code>true</code> if the button was pressed.
		 */
		public void setControllerInput(int pin, boolean pressed) {
			for (int i = 0; i < inputs.length; i++) {
				if (pin == inputPins[i]) {
					if (pressed) {
						inputs[i] = true;
						triggers[i] = true;
						timers[i] = delayTimer[i];
					} else {
						inputs[i] = false;
						triggers[i] = false;
						timers[i] = 0;
					}

				}
			}
		}

		/**
		 * If a controller is being used, this method is used to verify all of the pin
		 * inputs
		 */
		public void checkControllerInputs() {
			for (int i = 0; i < inputs.length; i++) {
				if (GPIO.digitalRead(inputPins[i]) == GPIO.HIGH) {
					inputs[i] = false;
					triggers[i] = false;
					timers[i] = 0;
				}
			}
		}

		/**
		 * Updates inputs and triggers for when a key is pressed
		 */
		public void setPressInput() {
			for (int i = 0; i < inputs.length; i++) {
				// not using the controller
				if (key == keyboardInputs[i]) {
					inputs[i] = true;
					triggers[i] = true;
					timers[i] = delayTimer[i];
				}
			}
		}

		/**
		 * Updates inputs and triggers for when a key is released
		 */
		public void setReleaseInput() {
			for (int i = 0; i < inputs.length; i++) {
				if (key == keyboardInputs[i]) {
					inputs[i] = false;
					triggers[i] = false;
					timers[i] = 0;
				}
			}
		}

		// READ_WRITE

		/**
		 * @return The inputs that are currently being pressed
		 */
		public boolean[] getInputs() {
			return inputs;
		}

		/**
		 * @param inputs The inputs that are currently being pressed
		 */
		public void setInputs(boolean[] inputs) {
			this.inputs = inputs;
		}

		/**
		 * @return The triggers that are active (the corresponding timer must expire in
		 *         order for these to be active)
		 */
		public boolean[] getTriggers() {
			return triggers;
		}

		/**
		 * @param triggers The triggers that are active (the corresponding timer must
		 *                 expire in order for these to be active)
		 */
		public void setTriggers(boolean[] triggers) {
			this.triggers = triggers;
		}

		/**
		 * @return The # of frames before the next input corresponding to the timer
		 */
		public int[] getTimers() {
			return timers;
		}

		/**
		 * @param timers The # of frames before the next input corresponding to the
		 *               timer
		 */
		public void setTimers(int[] timers) {
			this.timers = timers;
		}

		// READ_ONLY

		/**
		 * @return The ASCII values for the keyboard inputs
		 */
		public int[] getKeyboardInputs() {
			return keyboardInputs;
		}

		/**
		 * @return The ASCII values for the keyboard inputs
		 */
		public int[] getInputPins() {
			return inputPins;
		}

		/**
		 * @return # of frames between the input and the second trigger (the first is
		 *         immediate)
		 */
		public int[] getDelayTimer() {
			return delayTimer;
		}

		/**
		 * @return # of frames between every successive trigger after the second trigger
		 */
		public int[] getStartInputTimer() {
			return startInputTimer;
		}
	}

	/****************************************************************/

	/**
	 * This enum contains all of the player inputs that are required for the game to
	 * function.
	 * 
	 * @author Mark Sabbagh
	 * @author Logan Bowers
	 */
	public enum InputType {
		LEFT(0), RIGHT(1), UP(2), DOWN(3), A(4), B(5), NULL(6),;

		private int inputIndex; // The number associated with the input

		/**
		 * Constructs an Input
		 * 
		 * @param inputIndex The number associated with the input
		 */
		private InputType(int inputIndex) {
			this.inputIndex = inputIndex;
		}

		/**
		 * @param index The number associated with the input that this method returns
		 * @return The input at the corresponding index
		 */
		public static InputType get(int index) {
			for (InputType l : InputType.values()) {
				if (l.inputIndex == index)
					return l;
			}
			throw new IllegalArgumentException("Input not found");
		}

		/**
		 * @return The number associated with this input
		 */
		public int getIndex() {
			return inputIndex;
		}
	}

	/****************************************************************/

	/**
	 * This effect plays every time a line is cleared in the game.
	 * 
	 * @author Mark Sabbagh
	 */
	public class LineClearEffect extends Effect {

		private int flashTimer; // How many frames are remaining for a single line flash
		private float lineSize; // The size of a block in the line

		/**
		 * @param x            The x position of the upper left corner of the line
		 * @param y            The y position of the upper left corner of the line
		 * @param lifeTime     How long this effect lasts for
		 * @param runInIntense <code>true</code> if the method only runs in intense
		 * @param lineSize     The size of a block in the line
		 */
		LineClearEffect(float x, float y, int lifeTime, boolean runInIntense, float lineSize) {
			super(x, y, lifeTime, runInIntense);
			this.lineSize = lineSize;
			this.flashTimer = 0;
		}

		/*
		 * (non-Javadoc) Only need to decrease the life time
		 * 
		 * @see bowers_sabbagh.TetrisArcade.Effect#run()
		 */
		public void run() {
			lifeTime--;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see bowers_sabbagh.TetrisArcade.IDisplay#display(boolean)
		 */
		@Override
		public void display(boolean intenseMode) {

			rectMode(CORNER);
			noStroke();

			if (intenseMode) {
				// The color of the line on a gray scale is based on the lifetime
				fill(map(lifeTime, 10, 0, 255, 0));
				rect(x, y, lineSize * 10, lineSize);
			} else {
				// Line flashes white and black
				if (flashTimer <= 0) {
					flashTimer = 10;
				} else if (flashTimer <= 5) {
					fill(255);
				} else if (flashTimer <= 10) {
					fill(0);
				}

				rect(x, y, lineSize * 10, lineSize);

				flashTimer -= 2;
			}
		}

		// READ_WRITE

		/**
		 * @return The size of a block in the line
		 */
		public float getLineSize() {
			return lineSize;
		}

		/**
		 * @param lineSize The size of a block in the line
		 */
		public void setLineSize(float lineSize) {
			this.lineSize = lineSize;
		}

		// READ_ONLY

		/**
		 * @return How many frames are remaining for a single line flash
		 */
		public int getFlashTimer() {
			return flashTimer;
		}
	}

	/****************************************************************/

	/**
	 * @author Mark Sabbagh
	 */
	public class LineExplodeEffect extends Effect {

		private float vel; // How fast the explosion expands outward from the line
		private float dist; // How far the explosion is from its starting point

		/**
		 * @param x            The x position of the center of the explosion
		 * @param y            The y position of the center of the explosion
		 * @param lifeTime     How long this effect lasts for
		 * @param runInIntense <code>true</code> if the method only runs in intense
		 * @param vel          How fast the explosion expands outward from the line
		 */
		public LineExplodeEffect(float x, float y, int lifeTime, boolean runInIntense, float vel) {
			super(x, y, lifeTime, runInIntense);
			this.vel = vel;
			this.dist = 0;
		}

		/*
		 * (non-Javadoc) Updates the lifetime and distance of the explosion
		 * 
		 * @see bowers_sabbagh.TetrisArcade.Effect#run()
		 */
		public void run() {
			lifeTime--;
			dist += vel;
		}

		/*
		 * (non-Javadoc) Displays particles that expand outward based on the velocity
		 * 
		 * @see bowers_sabbagh.TetrisArcade.IDisplay#display(boolean)
		 */
		@Override
		public void display(boolean intenseMode) {
			if (intenseMode) {
				float deltaTheta = PI / 12;
				fill(45, 160);
				noStroke();
				pushMatrix();
				translate(x, y);

				for (int i = 0; i < 24; i++) {
					rotate(deltaTheta);
					rect(0, dist, 16, 16);
				}

				popMatrix();
			} else {
				println("Only runs in intense mode");
			}
		}

		// READ_WRITE

		/**
		 * @return How fast the explosion expands outward from the line
		 */
		public float getVel() {
			return vel;
		}

		/**
		 * @param vel How fast the explosion expands outward from the line
		 */
		public void setVel(float vel) {
			this.vel = vel;
		}

		// READ_ONLY

		/**
		 * @return How far the explosion is from its starting point
		 */
		public float getDist() {
			return dist;
		}
	}

	/****************************************************************/

	/**
	 * This class consists of code taken out of the Game Manager due to the
	 * complexity in dealing with logic specific to the menu.
	 * 
	 * @author Logan Bowers
	 */
	public class MenuManager implements IDisplay {
		private GameManager gameManager; // A reference to the game

		private MenuOptions selectedOption; // The option that is currently highlighted by the player
		private int selectedOptionX; // The button that is highlighted by the player (within an option)

		private Character[] nameCharacters; // The characters in the editable name displayed on the menu
		private Button[][] menuButtons; // All of the buttons associated with the menu

		/**
		 * Constructs the menu by creating all of the name characters and menu buttons
		 * 
		 * @param gameManager A reference to the game
		 */
		public MenuManager(GameManager gameManager) {
			this.gameManager = gameManager;

			this.selectedOption = MenuOptions.NAME;
			this.selectedOptionX = 0;

			// name
			this.nameCharacters = new Character[10];
			for (int i = 0; i < this.nameCharacters.length; i++) {
				if (i < gameManager.name.length()) {
					nameCharacters[i] = gameManager.name.charAt(i); // add all of the characters in the name
				} else {
					nameCharacters[i] = ' '; // add spaces for the rest of the characters
				}
			}

			// Menu Buttons
			this.menuButtons = new Button[3][2];
			for (int row = 0; row < menuButtons.length; row++) {
				for (int col = 0; col < menuButtons[0].length; col++) {
					this.menuButtons[row][col] = new Button(width / 4 + col * width / 2, height * (0.5f + row * 0.2f),
							25, 25, color(255, 70));
				}
			}

			this.updateButtons();
		}

		/**
		 * Updates the selected field of all of the buttons based on the menu state
		 */
		private void updateButtons() {
			for (int row = 0; row < this.menuButtons.length; row++) {
				for (int col = 0; col < this.menuButtons[0].length; col++) {
					if ((selectedOption.getIndex() - 1 == row) && (selectedOptionX == col)) {
						this.menuButtons[row][col].setSelected(true);
					} else {
						this.menuButtons[row][col].setSelected(false);
					}
				}
			}
		}

		/**
		 * Performs a function based on the player's input and the currently selected
		 * menu option
		 * 
		 * @param input The input that was passed by the game manager
		 */
		public void executeFunction(InputType input) {
			// There are 6 inputs. 2 inputs have different functions for 3 menu options. 1
			// input has a different function for all 4 menu options
			// So there is a total of 13 cases to deal with

			SoundManager sm = this.gameManager.soundManager; // shortens the code
			switch (input) {

			case LEFT:
				if (this.selectedOptionX > 0) {
					this.selectedOptionX--;
				} else {
					switch (this.selectedOption) {

					case LEVEL:
						if (gameManager.startLevel > 0) {
							gameManager.startLevel--;
						}
						break;

					case MUSIC:
						sm.musicVolume = Math.max(sm.musicVolume - 0.8f, sm.minVolume);
						sm.gameMusic.setGain(sm.musicVolume);
						if (sm.musicVolume == sm.minVolume) {
							sm.gameMusic.mute();
						}
						break;

					case SOUND:
						sm.soundVolume = Math.max(sm.soundVolume - 0.8f, sm.minVolume);
						for (AudioPlayer effect : sm.soundEffects.values()) {
							if (sm.soundVolume == sm.minVolume) {
								effect.mute();
							} else {
								effect.setGain(sm.soundVolume);
							}
						}

						break;
					default:
						break;
					}
				}
				updateButtons();
				break;

			case RIGHT:
				if (this.selectedOptionX < this.selectedOption.getLength() - 1) {
					this.selectedOptionX++;
				} else {

					switch (selectedOption) {

					case LEVEL:
						if (gameManager.startLevel < 16) {
							gameManager.startLevel++;
						}
						break;

					case MUSIC:
						sm.musicVolume = Math.min(sm.musicVolume + 0.8f, sm.maxVolume);
						sm.gameMusic.setGain(sm.musicVolume);
						if (sm.musicVolume > sm.minVolume) {
							sm.gameMusic.mute();
						}
						break;

					case SOUND:
						sm.soundVolume = Math.min(sm.soundVolume + 0.8f, sm.maxVolume);
						for (AudioPlayer effect : sm.soundEffects.values()) {
							if (sm.soundVolume <= sm.minVolume + 0.8f) {
								effect.unmute();
							} else {
								effect.setGain(sm.soundVolume);
							}
						}
						break;

					default:
						break;
					}
				}
				updateButtons();
				break;

			case UP:
				if (this.selectedOption.getIndex() > 0) {
					this.selectedOption = MenuOptions.get(this.selectedOption.getIndex() - 1);
				}

				updateButtons();
				break;

			case DOWN:
				if (this.selectedOption == MenuOptions.NAME) {
					this.selectedOptionX = 0;
				}
				if (this.selectedOption.getIndex() < MenuOptions.values().length - 1) {
					this.selectedOption = MenuOptions.get(this.selectedOption.getIndex() + 1);
				}

				updateButtons();
				break;

			case A:
				switch (this.selectedOption) {

				case NAME:
					char c = this.nameCharacters[this.selectedOptionX];
					switch (c) {
					// capital letters
					case 32:
						c = 65;
						break;
					// numbers
					case 90:
						c = 48;
						break;
					case 57:
						c = 32;
						break;
					default:
						c += 1;
						break;
					}
					this.nameCharacters[this.selectedOptionX] = c;

					gameManager.name = this.getNameFromChars();
					break;
				case LEVEL:
					if (this.selectedOptionX == 0) {
						if (gameManager.startLevel > 0) {
							gameManager.startLevel--;
						}
					} else {
						if (gameManager.startLevel < 15) {
							gameManager.startLevel++;
						}
					}
					break;

				case MUSIC:
					if (this.selectedOptionX == 0) {
						sm.musicVolume = Math.max(sm.musicVolume - 0.8f, sm.minVolume);
						if (sm.musicVolume == sm.minVolume) {
							sm.gameMusic.mute();
						}
					} else {
						if (sm.musicVolume == sm.minVolume) {
							sm.gameMusic.unmute();
						}
						sm.musicVolume = Math.min(sm.musicVolume + 0.8f, sm.maxVolume);
					}

					sm.gameMusic.setGain(sm.musicVolume);

					break;

				case SOUND:
					if (this.selectedOptionX == 0) {
						sm.soundVolume = Math.max(sm.soundVolume - 0.8f, sm.minVolume);
						if (sm.soundVolume == sm.minVolume) {
							for (AudioPlayer effect : sm.soundEffects.values()) {
								effect.mute();
							}
						}
					} else {
						sm.soundVolume = Math.min(sm.soundVolume + 0.8f, sm.maxVolume);
						if (sm.soundVolume == sm.minVolume) {
							for (AudioPlayer effect : sm.soundEffects.values()) {
								effect.unmute();
							}
						}
					}

					for (AudioPlayer effect : sm.soundEffects.values()) {
						effect.setGain(sm.soundVolume);
					}

					break;

				default:
					break;
				}

				break;
			case B:
				close();
				break;

			default:
				break;
			}
		}

		/**
		 * Constructs the name of the player using the characters in the menu
		 * 
		 * @return A string containing the player's name as determined by the menu
		 */
		public String getNameFromChars() {

			String tempName = "";

			for (Character nameChar : this.nameCharacters) {
				tempName = tempName.concat(nameChar.toString());
			}

			tempName = tempName.trim();

			return tempName;
		}

		/**
		 * Closes the menu and restarts the game if any fields were changed in menu.
		 */
		public void close() {
			gameManager.inputManager.changeDelays(false);
			gameManager.inMenu = false;
			if ((gameManager.startLevel != gameManager.oldStartLevel) || (gameManager.name != gameManager.oldName)) {
				gameManager.reset();
			} else {
				gameManager.soundManager.gameMusic.play();
			}
		}

		/*
		 * (non-Javadoc) Displays the menu contents onto the screen
		 * 
		 * @see bowers_sabbagh.TetrisArcade.IDisplay#display(boolean)
		 */
		@Override
		public void display(boolean intenseMode) {
			SoundManager sm = this.gameManager.soundManager;

			// background
			fill(0, 230);
			noStroke();
			rect(0, 0, width, height);

			// name
			float spacing = 3 * width / (4 * 10);
			textSize(width * 0.075f);
			for (int i = 0; i < this.nameCharacters.length; i++) {
				if ((this.selectedOption == MenuOptions.NAME) && (i == selectedOptionX)) {
					fill(0xff00eeff, 50);
					rectMode(CENTER);
					rect(width / 4 + i * spacing, height * 0.1f, width * 0.05f, height * 0.075f);
				}
				fill(255);
				text(this.nameCharacters[i], width / 4 + i * spacing, height * 0.1f);
			}

			// Level and Volume buttons
			for (int row = 0; row < this.menuButtons.length; row++) {
				for (int col = 0; col < this.menuButtons[0].length; col++) {
					this.menuButtons[row][col].display(intenseMode);
				}
			}

			// Level Header
			fill(255);
			textSize(width * 0.1f);
			text("Level ", width / 2, height * 0.35f);

			// Carots for level select
			fill(0);
			textSize(width * 0.05f);
			text("<", width / 4, height * 0.5f);
			text(">", 3 * width / 4, height * 0.5f);

			// Displaying the level number
			fill(255);
			textSize(width * 0.2f);
			text(gameManager.startLevel, width / 2, height * 0.5f);

			// Volume Header

			textSize(width * 0.1f);
			text("Music ", width / 2, height * 0.6f);

			// Slider for volume

			textSize(width * 0.05f);
			text("<", width / 4, height * 0.7f);
			text(">", 3 * width / 4, height * 0.7f);

			// line
			stroke(0xffdddddd, 150);
			strokeWeight(4);
			line(width / 3, height * 0.7f, 2 * width / 3, height * 0.7f);
			float x = map(sm.musicVolume, sm.minVolume, sm.maxVolume, width / 3, 2 * width / 3);

			// position
			noStroke();
			fill(255, 255);
			ellipse(x, height * 0.7f, 10, 10);

			// Sound Header

			textSize(width * 0.1f);
			text("Sound ", width / 2, height * 0.8f);

			// Slider for sound

			textSize(width * 0.05f);
			text("<", width / 4, height * 0.9f);
			text(">", 3 * width / 4, height * 0.9f);

			// line
			stroke(0xffdddddd, 150);
			strokeWeight(4);
			line(width / 3, height * 0.9f, 2 * width / 3, height * 0.9f);
			x = map(sm.soundVolume, sm.minVolume, sm.maxVolume, width / 3, 2 * width / 3);

			// position
			noStroke();
			fill(255, 255);
			ellipse(x, height * 0.9f, 10, 10);

			// Authors
			fill(255, 80);
			textSize(width * 0.04f);
			text("Mark Sabbagh\nLogan Bowers", width / 2, height * .94f);
		}

		// READ_WRITE

		/**
		 * @return The option that is currently highlighted by the player
		 */
		public MenuOptions getSelectedOption() {
			return selectedOption;
		}

		/**
		 * @param selectedOption The option that is currently highlighted by the player
		 */
		public void setSelectedOption(MenuOptions selectedOption) {
			this.selectedOption = selectedOption;
		}

		/**
		 * @return The button that is highlighted by the player (within an option)
		 */
		public int getSelectedOptionX() {
			return selectedOptionX;
		}

		/**
		 * @param selectedOptionX The button that is highlighted by the player (within
		 *                        an option)
		 */
		public void setSelectedOptionX(int selectedOptionX) {
			this.selectedOptionX = selectedOptionX;
		}

		/**
		 * @return The characters in the editable name displayed on the menu
		 */
		public Character[] getNameCharacters() {
			return nameCharacters;
		}

		/**
		 * @param nameCharacters The characters in the editable name displayed on the
		 *                       menu
		 */
		public void setNameCharacters(Character[] nameCharacters) {
			this.nameCharacters = nameCharacters;
		}

		// READ_ONLY

		/**
		 * @return A reference to the game
		 */
		public GameManager getGameManager() {
			return gameManager;
		}

		/**
		 * @return All of the buttons associated with the menu
		 */
		public Button[][] getMenuButtons() {
			return menuButtons;
		}
	}

	/****************************************************************/

	/**
	 * An enum containing options in the game menu
	 * 
	 * @author Logan Bowers
	 *
	 */
	public enum MenuOptions {
		NAME(0, 8), LEVEL(1, 2), MUSIC(2, 2), SOUND(3, 2),;

		private int optionIndex; // The number associated with the option

		private int optionLength; // The number of buttons that modify this option

		/**
		 * @param optionIndex The number associated with the option
		 * @param length      The number of buttons that modify this option
		 */
		private MenuOptions(int optionIndex, int length) {
			this.optionIndex = optionIndex;
			this.optionLength = length;
		}

		/**
		 * @param index The number associated with the option
		 * @return The option associated with the given index
		 */
		public static MenuOptions get(int index) {
			for (MenuOptions l : MenuOptions.values()) {
				if (l.optionIndex == index)
					return l;
			}
			throw new IllegalArgumentException("Option not found");
		}

		/**
		 * @return The index associated with this option
		 */
		public int getIndex() {
			return optionIndex;
		}

		/**
		 * @return The number of buttons within this option
		 */
		public int getLength() {
			return optionLength;
		}
	}

	/****************************************************************/

	/**
	 * This class is meant to represent a neural network with connection weights and
	 * neuron biases using the feed forward method of training. 
	 * It also implements back propagation to aid in training accuracy.
	 * <p>
	 * This class is no longer used in the project because the AI that we
	 * settled on is algorithmic and does not require neural networks, but we added
	 * it anyway because it took a long time to write.
	 * 
	 * @author Mark Sabbagh
	 *
	 */
	public class Network implements Serializable {
		public final int[] NETWORK_LAYER_SIZES;
		public final int INPUT_SIZE;
		public final int OUTPUT_SIZE;
		public final int NETWORK_SIZE;

		private double[][] outputs;
		private double[][][] weights;
		private double[][] biases;
		private double[][] errorSignals;
		private double[][] outputDerivatives;

		/**
		 * @param NETWORK_LAYER_SIZES
		 */
		public Network(int... NETWORK_LAYER_SIZES) {
			this.NETWORK_LAYER_SIZES = NETWORK_LAYER_SIZES;
			this.INPUT_SIZE = NETWORK_LAYER_SIZES[0];
			this.NETWORK_SIZE = NETWORK_LAYER_SIZES.length;
			this.OUTPUT_SIZE = NETWORK_LAYER_SIZES[NETWORK_SIZE - 1];

			// Initialize layers
			this.outputs = new double[NETWORK_SIZE][];
			this.weights = new double[NETWORK_SIZE][][];
			this.biases = new double[NETWORK_SIZE][];
			this.errorSignals = new double[NETWORK_SIZE][];
			this.outputDerivatives = new double[NETWORK_SIZE][];

			for (int i = 0; i < NETWORK_SIZE; i++) {
				this.outputs[i] = new double[NETWORK_LAYER_SIZES[i]];
				this.errorSignals[i] = new double[NETWORK_LAYER_SIZES[i]];
				this.outputDerivatives[i] = new double[NETWORK_LAYER_SIZES[i]];

				this.biases[i] = createRandomArray(NETWORK_LAYER_SIZES[i], .3, .7);

				if (i > 0)
					weights[i] = createRandomArray(NETWORK_LAYER_SIZES[i], NETWORK_LAYER_SIZES[i - 1], -.5, .5);
			}
		}

		// Runs inputs and returns outputs
		/**
		 * @param input
		 * @return
		 */
		public double[] feedForward(double... input) {

			if (input.length != this.INPUT_SIZE) {
				println("Feed Forward Failed -> Invalid input dimention: " + input.length);
				return null;
			}

			this.outputs[0] = input;

			for (int layer = 1; layer < NETWORK_SIZE; layer++) {
				for (int neuron = 0; neuron < NETWORK_LAYER_SIZES[layer]; neuron++) {

					double sum = biases[layer][neuron];

					for (int prevNeuron = 0; prevNeuron < NETWORK_LAYER_SIZES[layer - 1]; prevNeuron++) {
						sum += outputs[layer - 1][prevNeuron] * weights[layer][neuron][prevNeuron];
					}

					outputs[layer][neuron] = sigmoid(sum);
					outputDerivatives[layer][neuron] = outputs[layer][neuron] * (1 - outputs[layer][neuron]);
				}
			}
			return outputs[NETWORK_SIZE - 1];
		}

		/**
		 * @param inputs
		 * @param targets
		 * @param eta
		 */
		public void train(double[] inputs, double[] targets, double eta) {
			if (inputs.length != INPUT_SIZE) {
				println("Training Failed -> Invalid input dimention: " + inputs.length);
				return;
			}

			if (targets.length != OUTPUT_SIZE) {
				println("Training Failed -> Invalid target dimention: " + targets.length);
				return;
			}

			feedForward(inputs);
			backPropError(targets);
			updateWeights(eta);
		}

		/**
		 * @param targets
		 */
		public void backPropError(double[] targets) {

			for (int neuron = 0; neuron < NETWORK_LAYER_SIZES[NETWORK_SIZE - 1]; neuron++) {
				errorSignals[NETWORK_SIZE - 1][neuron] = (outputs[NETWORK_SIZE - 1][neuron] - targets[neuron])
						* outputDerivatives[NETWORK_SIZE - 1][neuron];
			}

			for (int layer = NETWORK_SIZE - 2; layer > 0; layer--) {
				for (int neuron = 0; neuron < NETWORK_LAYER_SIZES[layer]; neuron++) {

					double sum = 0;
					for (int nextNeuron = 0; nextNeuron < NETWORK_LAYER_SIZES[layer + 1]; nextNeuron++) {
						sum += weights[layer + 1][nextNeuron][neuron] * errorSignals[layer + 1][nextNeuron];
					}

					this.errorSignals[layer][neuron] = sum * outputDerivatives[layer][neuron];
				}
			}
		}

		/**
		 * @param eta
		 */
		public void updateWeights(double eta) {
			for (int layer = 1; layer < NETWORK_SIZE; layer++) {
				for (int neuron = 0; neuron < NETWORK_LAYER_SIZES[layer]; neuron++) {

					double delta = -eta * errorSignals[layer][neuron];
					biases[layer][neuron] += delta;

					for (int prevNeuron = 0; prevNeuron < NETWORK_LAYER_SIZES[layer - 1]; prevNeuron++) {
						weights[layer][neuron][prevNeuron] += delta * outputs[layer - 1][prevNeuron];
					}
				}
			}
		}

		/**
		 * @param x
		 * @return
		 */
		private double sigmoid(double x) {
			return 1d / (1 + Math.exp(-x));
		}

		/**
		 * @param fileName
		 * @throws Exception
		 */
		public void saveNetwork(String fileName) throws Exception {

			XML xml = new XML(fileName);
			XML net = xml.addChild("network");

			for (int layerIndex = 1; layerIndex < NETWORK_SIZE; layerIndex++) {
				XML layer = net.addChild("layer" + layerIndex);

				for (int neuronIndex = 0; neuronIndex < NETWORK_LAYER_SIZES[layerIndex]; neuronIndex++) {
					XML neuron = layer.addChild("neuron" + neuronIndex);
					// bias[layerIndex][neuronIndex];
					XML bias = neuron.addChild("bias");
					bias.setDouble("biasValue", biases[layerIndex][neuronIndex]);
					for (int prevNeuronIndex = 0; prevNeuronIndex < NETWORK_LAYER_SIZES[layerIndex
							- 1]; prevNeuronIndex++) {
						// weights[layerIndex][neuron][prevNeuron];
						XML weight = neuron.addChild("weights" + prevNeuronIndex);
						weight.setDouble("weightValue", this.weights[layerIndex][neuronIndex][prevNeuronIndex]);
					}
				}
			}

			saveXML(xml, "data/" + fileName);

			println("SAVED NETWORK");
		}

		/**
		 * @param fileName
		 * @throws Exception
		 */
		public void loadNetwork(String fileName) throws Exception {
			try {
				XML xml = loadXML(fileName);
				XML net = xml.getChild("network");

				for (int layerIndex = 1; layerIndex < this.NETWORK_SIZE; layerIndex++) {
					XML layer = net.getChild("layer" + layerIndex);

					for (int neuronIndex = 0; neuronIndex < this.NETWORK_LAYER_SIZES[layerIndex]; neuronIndex++) {
						XML neuron = layer.getChild("neuron" + neuronIndex);
						this.biases[layerIndex][neuronIndex] = neuron.getChild("bias").getDouble("biasValue");

						for (int prevNeuronIndex = 0; prevNeuronIndex < this.NETWORK_LAYER_SIZES[layerIndex
								- 1]; prevNeuronIndex++) {
							this.weights[layerIndex][neuronIndex][prevNeuronIndex] = neuron
									.getChild("weights" + prevNeuronIndex).getDouble("weightValue");
						}
					}
				}
			} catch (Exception E) {
				E.printStackTrace();
				println("Could not load network");
			}
		}

		// Rate: 0 - 1
		/**
		 * @param rate
		 * @param eta
		 * @return
		 */
		public Network createAlter(float rate, float eta) {
			Network net = new Network(NETWORK_LAYER_SIZES);

			for (int layer = 1; layer < NETWORK_SIZE; layer++) {
				for (int neuron = 0; neuron < NETWORK_LAYER_SIZES[layer]; neuron++) {

					net.biases[layer][neuron] = Double.valueOf(biases[layer][neuron]);

					if (randomValue(0, 1) < rate)
						net.biases[layer][neuron] += randomValue(-eta, eta);

					for (int prevNeuron = 0; prevNeuron < NETWORK_LAYER_SIZES[layer - 1]; prevNeuron++) {
						net.weights[layer][neuron][prevNeuron] = Double.valueOf(weights[layer][neuron][prevNeuron]);

						if (randomValue(0, 1) < rate)
							net.weights[layer][neuron][prevNeuron] += randomValue(-eta, eta);
					}
				}
			}

			return net;
		}

		// Network tools
		/**
		 * @param size
		 * @param init_value
		 * @return
		 */
		private double[] createArray(int size, double init_value) {
			if (size < 1) {
				return null;
			}
			double[] ar = new double[size];
			for (int i = 0; i < size; i++) {
				ar[i] = init_value;
			}
			return ar;
		}

		/**
		 * @param size
		 * @param lower_bound
		 * @param upper_bound
		 * @return
		 */
		private double[] createRandomArray(int size, double lower_bound, double upper_bound) {
			if (size < 1) {
				return null;
			}
			double[] ar = new double[size];
			for (int i = 0; i < size; i++) {
				ar[i] = randomValue(lower_bound, upper_bound);
			}
			return ar;
		}

		/**
		 * @param sizeX
		 * @param sizeY
		 * @param lower_bound
		 * @param upper_bound
		 * @return
		 */
		private double[][] createRandomArray(int sizeX, int sizeY, double lower_bound, double upper_bound) {
			if (sizeX < 1 || sizeY < 1) {
				return null;
			}
			double[][] ar = new double[sizeX][sizeY];
			for (int i = 0; i < sizeX; i++) {
				ar[i] = createRandomArray(sizeY, lower_bound, upper_bound);
			}
			return ar;
		}

		/**
		 * @param lower_bound
		 * @param upper_bound
		 * @return
		 */
		private double randomValue(double lower_bound, double upper_bound) {
			return Math.random() * (upper_bound - lower_bound) + lower_bound;
		}

		/**
		 * @param lowerBound
		 * @param upperBound
		 * @param amount
		 * @return
		 */
		private Integer[] randomValues(int lowerBound, int upperBound, int amount) {

			lowerBound--;

			if (amount > (upperBound - lowerBound)) {
				return null;
			}

			Integer[] values = new Integer[amount];
			for (int i = 0; i < amount; i++) {
				int n = (int) (Math.random() * (upperBound - lowerBound + 1) + lowerBound);
				while (containsValue(values, n)) {
					n = (int) (Math.random() * (upperBound - lowerBound + 1) + lowerBound);
				}
				values[i] = n;
			}
			return values;
		}

		/**
		 * @param ar
		 * @param value
		 * @return
		 */
		private <T extends Comparable<T>> boolean containsValue(T[] ar, T value) {
			for (int i = 0; i < ar.length; i++) {
				if (ar[i] != null) {
					if (value.compareTo(ar[i]) == 0) {
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * @param values
		 * @return
		 */
		private int indexOfHighestValue(double[] values) {
			int index = 0;
			for (int i = 1; i < values.length; i++) {
				if (values[i] > values[index]) {
					index = i;
				}
			}
			return index;
		}
	}

	/**
	 * A class that represents a single piece within the game, including the blocks
	 * that make up the piece and the type and orientation of the piece.
	 * 
	 * @author Logan Bowers
	 *
	 */
	public class Piece implements IDisplay {

		private Block[] blocks; // The blocks displayed to the grid that are associated with this piece
		private float[][] relPos; // The relative positions of each block with reference to the center of the
									// piece
		private float x; // The x position of the center of the piece
		private float y; // The y position of the center of the piece
		private PieceType type; // The shape of the piece on the grid
		private float size; // The size of the piece blocks
		private int[] pieceColor; // The piece color in normal and intense mode
		private int orientation; // How many times the piece has been rotated by 90 degrees
		private LinkedList<PointXY> trail; // Used for the trail effect in intense mode

		private boolean locked; // After the piece collides, this becomes true as the piece cannot be moved
								// anymore

		/**
		 * Default constructor. Do not use for practical purposes
		 */
		public Piece() {

			this.relPos = new float[blocks.length][2];
			this.x = 5;
			this.y = 0;
			this.type = PieceType.I;
			this.size = 20;
			this.pieceColor = new int[2];
			this.pieceColor[0] = 255;
			this.pieceColor[1] = 255;
			this.orientation = 0;
			this.trail = new LinkedList<PointXY>();

			this.locked = false;
			this.blocks = this.generateBlocks();
		}

		/**
		 * Constructs a piece with the given values
		 * 
		 * @param x          The x position of the center of the piece
		 * @param y          The y position of the center of the piece
		 * @param type       The shape of the piece on the grid
		 * @param size       The size of the piece blocks
		 * @param pieceColor The piece color in normal and intense mode
		 * @param locked     After the piece collides, this becomes true as the piece
		 *                   cannot be moved anymore
		 */
		public Piece(float x, float y, PieceType type, float size, int[] pieceColor, boolean locked) {
			this.x = x;
			this.y = y;
			this.type = type;
			this.size = size;
			this.pieceColor = pieceColor;
			this.orientation = 0;
			this.trail = new LinkedList<PointXY>();

			this.locked = locked;
			this.blocks = this.generateBlocks();
		}

		/*
		 * (non-Javadoc) Creates a new piece object with the same fields as this piece
		 * 
		 * @see java.lang.Object#clone()
		 */
		public Piece clone() {
			Piece tempPiece = new Piece(x, y, type, size, pieceColor, locked);
			tempPiece.x = 5;

			return tempPiece;

		}

		/**
		 * Generates this piece's blocks using the piece type and the relative positions
		 * of the pieces
		 * 
		 * @return The piece blocks
		 */
		private Block[] generateBlocks() {
			Block[] temp = new Block[4];
			switch (this.type) {
			case I:
				this.relPos = new float[][] { { -2, 0 }, { -1, 0 }, { 0, 0 }, { 1, 0 } };

				break;
			case O:
				this.relPos = new float[][] { { -0.5f, -0.5f }, { 0.5f, -0.5f }, { 0.5f, 0.5f }, { -0.5f, 0.5f } };

				break;
			case L:
				this.relPos = new float[][] { { -1, 0 }, { 0, 0 }, { 1, 0 }, { -1, 1 } };

				break;
			case J:
				this.relPos = new float[][] { { -1, 0 }, { 0, 0 }, { 1, 0 }, { 1, 1 } };

				break;
			case S:
				this.relPos = new float[][] { { 0.5f, -0.5f }, { -0.5f, -0.5f }, { -0.5f, 0.5f }, { -1.5f, 0.5f } };

				break;
			case Z:
				this.relPos = new float[][] { { -1.5f, -0.5f }, { -0.5f, -0.5f }, { -0.5f, 0.5f }, { 0.5f, 0.5f } };

				break;
			case T:
				this.relPos = new float[][] { { -1, 0 }, { 0, 0 }, { 1, 0 }, { 0, 1 } };

				break;
			default:
				println("invalid type of block");
			}

			for (int i = 0; i < temp.length; i++) {
				temp[i] = new Block(Math.round(this.x + this.relPos[i][0]), Math.round(this.y + this.relPos[i][1]),
						this.size, this.pieceColor, this.type);
			}

			return temp;
		}

		/**
		 * Updates each block's position and color. This is usually called whenever
		 * fields are changed in the piece class
		 */
		public void updateBlocks() {
			// Updating each block's position relative to the piece's position
			for (int i = 0; i < this.blocks.length; i++) {
				// Using round to convert from float to int
				this.blocks[i].setPos(Math.round(this.x + this.relPos[i][0]), Math.round(this.y + this.relPos[i][1]));
				this.blocks[i].setColor(this.pieceColor);
			}
		}

		/**
		 * Rotates the piece by 90 degrees
		 * 
		 * @param cw <code>true</code> if the piece rotates clockwise.
		 *           <code>false</code> if counterclockwise
		 */
		public void rotate(boolean cw) {
			// rewrite

			for (int i = 0; i < this.relPos.length; i++) {
				// swapping the coordinates
				float temp = this.relPos[i][0];
				this.relPos[i][0] = this.relPos[i][1];
				this.relPos[i][1] = temp;

				if (cw)
					this.relPos[i][0] = -this.relPos[i][0];
				else
					this.relPos[i][1] = -this.relPos[i][1];
			}

			if (cw)
				this.orientation = (this.orientation + 1) % 4;
			else
				this.orientation = (this.orientation + 3) % 4;

			this.updateBlocks();
		}

		/**
		 * Creates a trail effect that extends outward from the piece during intense
		 * mode
		 * <p>
		 * Written by Mark Sabbagh
		 */
		public void drawTrail() {
			// Get the current point and add it to the front of the list
			float posX = x * size;
			float posY = y * size;

			if (trail.size() == 0 || (trail.getFirst().x != posX || trail.getFirst().y != posY)) {
				PointXY p = new PointXY(x * size, y * size);
				trail.addFirst(p);

				// If trail is too 'long' remove the oldest points
				while (trail.size() > 15)
					trail.removeLast();
			}

			// Draw trail if there are at least 2 points
			if (trail.size() >= 2) {
				PointXY currPoint;
				for (int i = 0; i < trail.size(); i++) {

					currPoint = trail.get(i);

					fill(40, 150 - (10 * i));
					rect(currPoint.x, currPoint.y, 15 - i, 15 - i);
				}
			}
		}

		/*
		 * (non-Javadoc) Displays the blocks inside this piece
		 * 
		 * @see bowers_sabbagh.TetrisArcade.IDisplay#display(boolean)
		 */
		public void display(boolean intenseMode) {
			if (intenseMode)
				drawTrail();
			for (Block block : this.blocks) {
				block.display(intenseMode);
			}
		}

		// READ_WRITE

		/**
		 * @return An array containing the coordinates of the center of the piece
		 */
		public float[] getPos() {
			float[] pos = { this.x, this.y };
			return pos;
		}

		/**
		 * @param x The x position of the center of the piece
		 * @param y The y position of the center of the piece
		 */
		public void setPos(float x, float y) {
			this.x = x;
			this.y = y;
			this.updateBlocks();
		}

		/**
		 * @return The x position of the center of the piece
		 */
		public float getX() {
			return x;
		}

		/**
		 * @param x The x position of the center of the piece
		 */
		public void setX(float x) {
			this.x = x;
			this.updateBlocks();
		}

		/**
		 * @return The y position of the center of the piece
		 */
		public float getY() {
			return y;
		}

		/**
		 * @param y The y position of the center of the piece
		 */
		public void setY(float y) {
			this.y = y;
			this.updateBlocks();
		}

		/**
		 * @return The shape of the piece on the grid
		 */
		public PieceType getType() {
			return type;
		}

		/**
		 * @param type The shape of the piece on the grid
		 */
		public void setType(PieceType type) {
			this.type = type;
			this.generateBlocks();
			this.updateBlocks();
		}

		/**
		 * @return The size of the piece blocks
		 */
		public float getSize() {
			return size;
		}

		/**
		 * @param size The size of the piece blocks
		 */
		public void setSize(float size) {
			this.size = size;
		}

		/**
		 * @return The piece color in normal and intense mode
		 */
		public int[] getPieceColor() {
			return pieceColor;
		}

		/**
		 * @param pieceColor The piece color in normal and intense mode
		 */
		public void setPieceColor(int[] pieceColor) {
			this.pieceColor = pieceColor;
			this.updateBlocks();
		}

		// READ_ONLY

		/**
		 * @return After the piece collides, this becomes true as the piece cannot be
		 *         moved anymore
		 */
		public boolean isLocked() {
			return locked;
		}

		/**
		 * @return The blocks displayed to the grid that are associated with this piece
		 */
		public Block[] getBlocks() {
			return blocks;
		}

		/**
		 * @return The relative positions of each block with reference to the center of
		 *         the piece
		 */
		public float[][] getRelPos() {
			return relPos;
		}

		/**
		 * @return How many times the piece has been rotated by 90 degrees
		 */
		public int getOrientation() {
			return orientation;
		}

		/**
		 * @return Used for the trail effect in intense mode
		 */
		public LinkedList<PointXY> getTrail() {
			return trail;
		}
	}

	/****************************************************************/

	/**
	 * An enum containing all possible piece types that a game piece can have
	 * 
	 * @author Logan Bowers
	 * @see Piece
	 */
	public enum PieceType {
		I(0), O(1), L(2), J(3), S(4), Z(5), T(6), NULL(7);

		private int pieceIndex; // The number associated with the piece type

		/**
		 * @param index The number associated with the piece type
		 */
		private PieceType(int index) {
			this.pieceIndex = index;
		}

		/**
		 * @param index The number associated with the piece type
		 * @return The piece type at the given index
		 */
		public static PieceType get(int index) {
			for (PieceType l : PieceType.values()) {
				if (l.pieceIndex == index)
					return l;
			}
			throw new IllegalArgumentException("Piece not found");
		}

		/**
		 * @return The number associated with this piece type
		 */
		public int getIndex() {
			return pieceIndex;
		}
	}

	/****************************************************************/

	/**
	 * A representation of a 2D point in space
	 * 
	 * @author Mark Sabbagh
	 *
	 */
	public class PointXY {
		private float x, y;

		/**
		 * @param px The x value of the point
		 * @param py The y value of the point
		 */
		public PointXY(float px, float py) {
			x = px;
			y = py;
		}

		/**
		 * @return The x value of the point
		 */
		public float getX() {
			return x;
		}

		/**
		 * @param x The x value of the point
		 */
		public void setX(float x) {
			this.x = x;
		}

		/**
		 * @return The y value of the point
		 */
		public float getY() {
			return y;
		}

		/**
		 * @param y The y value of the point
		 */
		public void setY(float y) {
			this.y = y;
		}
	}

	/****************************************************************/

	/**
	 * This class is a subclass of the game manager and manages music and sound
	 * effects for the game
	 * 
	 * @author Logan Bowers
	 *
	 */
	public class SoundManager {
		/*
		 * Constants
		 */

		private final int minVolume = -20; // The minimum decibel volume of the music and sound
		private final int maxVolume = 3; // The maximum decibel volume of the music and sound

		/*
		 * Fields
		 */

		private Minim minim; // This is required to create audio players
		private Map<String, AudioPlayer> soundEffects; // A mapping of effect names to the player that plays the effect
		private AudioPlayer gameMusic; // The music that is played in game
		private int currentMusic; // The position of the game music, 0 is the beginning, 1 is the intense loop,
									// and 2 is the classic loop
		private int[] transitionPoints = { 26975, 51000, 86959 }; // Positions in milliseconds of the intense section,
																	// classic section, and end of the song
		private float musicVolume; // The decibel volume of the music
		private float soundVolume; // The decibel volume of the sound

		/**
		 * Default Constructor : loads all of the music and sound effects and sets
		 * initial values for the volume
		 * 
		 * @param minim This comes from the game manager and is required to create audio
		 *              players
		 */
		public SoundManager(Minim minim) {
			this.minim = minim;

			// adding all of the sound effects
			this.soundEffects = new HashMap<String, AudioPlayer>();

			// Jeff Mode
			for (int i = 0; i < 5; i++) {
				this.soundEffects.put("BoomTetrisForJeff" + i, this.minim.loadFile("BoomTetrisForJeff" + i + ".mp3"));
			}
			this.soundEffects.put("NeckandNeck", this.minim.loadFile("NeckandNeck.mp3"));
			this.soundEffects.put("Top2", this.minim.loadFile("Top2.mp3"));
			this.soundEffects.put("Drought", this.minim.loadFile("Drought.mp3"));
			this.soundEffects.put("LongBar", this.minim.loadFile("LongBar.mp3"));
			this.soundEffects.put("Lost", this.minim.loadFile("NoLongBar.mp3"));
			this.soundEffects.put("IntenseBoom", this.minim.loadFile("IntenseBoom.mp3"));

			// Not Jeff Mode
			this.soundEffects.put("Drop", this.minim.loadFile("ClassicDrop.mp3"));
			this.soundEffects.put("IntenseDrop", this.minim.loadFile("ModernDrop.mp3"));
			this.soundEffects.put("Clear", this.minim.loadFile("ClassicClear.mp3"));
			this.soundEffects.put("IntenseClear", this.minim.loadFile("ModernClear.mp3"));
			this.soundEffects.put("Tetris", this.minim.loadFile("ClassicTetris.mp3"));
			this.soundEffects.put("IntenseTetris", this.minim.loadFile("ModernTetris.mp3"));

			// sound effects volume
			this.soundVolume = maxVolume;
			for (AudioPlayer effect : this.soundEffects.values()) {
				effect.setGain(this.soundVolume);
			}

			// music
			this.gameMusic = this.minim.loadFile("TetrisGame.wav");
			this.currentMusic = 0;

			// music volume
			this.musicVolume = maxVolume;
			this.gameMusic.setGain(this.musicVolume);

		}

		/**
		 * Starts the game music from the intro
		 */
		public void startMusicFromBeginning() {
			this.rewindMusic();
			this.currentMusic = 0;
			this.gameMusic.play();
		}

		/**
		 * Starts the game music from the first transition points
		 */
		public void startMusicFromLoop() {
			this.rewindMusic();
			this.gameMusic.skip(this.transitionPoints[0]);
			this.currentMusic = 1;
			this.gameMusic.play();
		}

		/**
		 * Rewinds the game music to the intro
		 */
		public void rewindMusic() {
			this.gameMusic.pause();
			this.gameMusic.rewind();
		}

		/**
		 * Plays the sound effect corresponding to the name of the effect and the game
		 * mode
		 * 
		 * @param effectName  The name of the effect
		 * @param intenseMode <code>true</code> if in intense mode
		 */
		public void playSoundEffect(String effectName, boolean intenseMode) {
			if (this.soundEffects.get(effectName) != null) {
				// The effect exists
				if (intenseMode) {
					// Play the intense mode effect
					rewindSoundEffect("Intense" + effectName);
					this.soundEffects.get("Intense" + effectName).play();
				} else {
					// Play the normal mode effect
					rewindSoundEffect(effectName);
					this.soundEffects.get(effectName).play();
				}
			} else {
				println("Error playing sound effect");
			}
		}

		/**
		 * Rewinds the sound effect with the effect name given
		 * 
		 * @param effectName The name of the effect to rewind
		 */
		public void rewindSoundEffect(String effectName) {
			this.soundEffects.get(effectName).rewind();
			this.soundEffects.get(effectName).rewind();
		}

		/**
		 * Rewinds all sound effects to the beginning.
		 */
		public void rewindAllSoundEffects() {
			for (AudioPlayer effect : this.soundEffects.values()) {
				if ((effect != null) && (effect.position() > effect.length() - 10)) {
					effect.rewind();
					effect.pause();
				}
			}
		}

		// READ_WRITE

		/**
		 * @return The music that is played in game
		 */
		public AudioPlayer getGameMusic() {
			return gameMusic;
		}

		/**
		 * @param gameMusic The music that is played in game
		 */
		public void setGameMusic(AudioPlayer gameMusic) {
			this.gameMusic = gameMusic;
		}

		/**
		 * @return The position of the game music, 0 is the beginning, 1 is the intense
		 *         loop, and 2 is the classic loop
		 */
		public int getCurrentMusic() {
			return currentMusic;
		}

		/**
		 * @param currentMusic The position of the game music, 0 is the beginning, 1 is
		 *                     the intense loop, and 2 is the classic loop
		 */
		public void setCurrentMusic(int currentMusic) {
			this.currentMusic = currentMusic;
		}

		/**
		 * @return Positions in milliseconds of the intense section, classic section,
		 *         and end of the song
		 */
		public int[] getTransitionPoints() {
			return transitionPoints;
		}

		/**
		 * @param transitionPoints Positions in milliseconds of the intense section,
		 *                         classic section, and end of the song
		 */
		public void setTransitionPoints(int[] transitionPoints) {
			this.transitionPoints = transitionPoints;
		}

		/**
		 * @return The decibel volume of the music
		 */
		public float getMusicVolume() {
			return musicVolume;
		}

		/**
		 * @param musicVolume The decibel volume of the music
		 */
		public void setMusicVolume(float musicVolume) {
			this.musicVolume = musicVolume;
		}

		/**
		 * @return The decibel volume of the sound
		 */
		public float getSoundVolume() {
			return soundVolume;
		}

		/**
		 * @param soundVolume The decibel volume of the sound
		 */
		public void setSoundVolume(float soundVolume) {
			this.soundVolume = soundVolume;
		}

		// READ_ONLY

		/**
		 * @return The minimum decibel volume of the music and sound
		 */
		public int getMinVolume() {
			return minVolume;
		}

		/**
		 * @return The maximum decibel volume of the music and sound
		 */
		public int getMaxVolume() {
			return maxVolume;
		}

		/**
		 * @return This is required to create audio players
		 */
		public Minim getMinim() {
			return minim;
		}

		/**
		 * @return A mapping of effect names to the player that plays the effect
		 */
		public Map<String, AudioPlayer> getSoundEffects() {
			return soundEffects;
		}
	}

	/****************************************************************/

	/**
	 * Displays moving particles across the screen during intense mode
	 * 
	 * @author Mark Sabbagh
	 */
	public class StreakEffect extends Effect {

		float vel; // How fast the particle moves across the screen

		/**
		 * @param x            The x position of the particle
		 * @param y            The y position of the particle
		 * @param lifeTime     The # of frames that the effect lasts for
		 * @param runInIntense Whether the effect only runs in intense
		 * @param vel          How fast the particle moves across the screen
		 */
		StreakEffect(float x, float y, int lifeTime, boolean runInIntense, float vel) {
			super(x, y, lifeTime, runInIntense);
			this.vel = vel;
		}

		/*
		 * (non-Javadoc) Changes the y position of the particle according to the
		 * velocity
		 * 
		 * @see bowers_sabbagh.TetrisArcade.Effect#run()
		 */
		@Override
		public void run() {
			lifeTime--;
			y -= vel;
		}

		/*
		 * (non-Javadoc) Displays the particle to the screen
		 * 
		 * @see bowers_sabbagh.TetrisArcade.IDisplay#display(boolean)
		 */
		@Override
		public void display(boolean intenseMode) {
			if (intenseMode) {
				fill(45, 120);
				ellipse(x, y, 5, 5);
			} else {
				println("Only runs in intense mode");
			}
		}

		/**
		 * @return How fast the particle moves across the screen
		 */
		public float getVel() {
			return vel;
		}

		/**
		 * @param vel How fast the particle moves across the screen
		 */
		public void setVel(float vel) {
			this.vel = vel;
		}
	}

	/**
	 * This program's main calls the main associated with PApplet and inserts
	 * "packageName.nameOfProgram" as the PApplet's arguments.
	 * 
	 * @param args Arguments passed to the main method (generally left as
	 *             <code>null</code>).
	 */
	public static void main(String[] args) {
		String[] appletArgs = new String[] { TetrisArcade.class.getPackage().getName() + ".TetrisArcade" };

		if (args != null) {
			PApplet.main(concat(appletArgs, args));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
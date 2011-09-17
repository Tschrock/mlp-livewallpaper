package com.overkill.live.pony;


import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;

/**
 * This class handles the {@link Sprite} movement and frame-animation
 * @author ov3rk1ll
 *
 */
public class Pony{
	private static final int MOVEMENT_DELAY_MS = 50;
	
	public String name;
	
	private Point position;	
	private Point destination;
	
	//private AllowedMoves movement;
	public ArrayList<Behavior> behaviors;
	private Behavior current_behavior = null;
	private boolean ponyDirection;

	private long lastTimeMoved = 0;
	
	private boolean hasSpawned = false;

	private Behavior previous_behavior;

	//private float largestSizeX;

	//private int largestSizeY;

	//private boolean AtDestination;
	
	public enum AllowedMoves {
		None,
	    Horizontal_Only,
	    Vertical_Only,
	    Horizontal_Vertical,
	    Diagonal_Only,
	    Diagonal_Horizontal,
	    Diagonal_Vertical,
	    All,
	    MouseOver,
	    Sleep,
	    Dragged
	}

	public enum Directions{
	    top,
	    bottom,
	    left,
	    right,
	    bottom_right,
	    bottom_left,
	    top_right,
	    top_left,
	    center,
	    random,
	    random_not_center
	}

	public Pony(String name){
		this.name = name;
		this.behaviors = new ArrayList<Behavior>();
		this.position = new Point(0, 0);
	}

	public void update(final long globalTime) {
		if (current_behavior == null) { // If we have no behavior, select a random one
			selectBehavior(null, globalTime);
		} else if ((current_behavior.endTime - globalTime) <= 0) { // If the behavior has run its course, select a new one			
			if(MyLittleWallpaperService.DEBUG_RENDERTIME) Log.i("Pony[" + name + "]", "Current Behavior ended");
			if (current_behavior.linkedBehavior != null) { // If we have a linked behavior, select that one next
				selectBehavior(current_behavior.linkedBehavior, globalTime);
			} else { // Otherwise select a random one
				selectBehavior(null, globalTime);
			}					
		}   	        
	    // Move the Pony
		if(hasSpawned == false){
			this.teleport();
			hasSpawned = true;
		}
	    move(globalTime);
	    current_behavior.update(globalTime);
	}
	
	public void teleport() {
		Point teleport_location = new Point();		
		for (int i = 0; i < 300; i++) {
			// Then select a random location to appear at		
		    teleport_location = new Point(MyLittleWallpaperService.rand.nextInt((int)RenderEngine.screenBounds.width()) + (int)RenderEngine.screenBounds.left, MyLittleWallpaperService.rand.nextInt((int)RenderEngine.screenBounds.height()) + (int)RenderEngine.screenBounds.top);
		
		    if(isPonyOnWallpaper(teleport_location) == true) break;
		}	
	    // Finally, go there
	    this.position = teleport_location;
	}

	
	public void move(long globalTime) {		
		if(globalTime - lastTimeMoved < MOVEMENT_DELAY_MS) // we want to move again to quickly
			return;
		
		lastTimeMoved = globalTime;
		
		double speed = current_behavior.speed;
				
		// Get the destination for following (will be blank(0,0) if not following)
		destination = current_behavior.getDestination(RenderEngine.screenBounds.width(), RenderEngine.screenBounds.height());
		
		// Calculate the movement speed
		int x_movement = 0;
		int y_movement = 0;
		
	    if (!current_behavior.right) {
	    	speed = -speed;
	    }
	    
	    // Calculate the movement speed normally
		x_movement = (int) (current_behavior.horizontal ? speed : 0);
		y_movement = (int) (current_behavior.vertical ? speed : 0);
		
		if (current_behavior.up) {
			if (y_movement > 0) {
				y_movement = -y_movement;
			}
		} else {
			if (y_movement < 0) {
				y_movement = -y_movement;
			}
		}
	    		
		// Point to determine where we would end up at this speed
		Point new_location = new Point(position.x + x_movement, position.y + y_movement);
		
	
	    if (isPonyOnWallpaper(new_location)) {
	        position = new_location;   
		    //paint(globalTime);     
	        return;
	    }
	
	    //Nothing to worry about, we are on screen, but our current behavior would take us 
	    // off-screen in the next move.  Just do something else.
	    // if we are moving to a destination, our path is blocked, and we need to abort the behavior
	    // if we are just moving normally, just "bounce" off of the barrier.
	
	    if (destination.x == 0 && destination.y == 0) {
	    	current_behavior.bounce(this, position, new_location, x_movement, y_movement);
	    }
	    
	}
		
	public void touch() {
		current_behavior = behaviors.get(0);
		for (Behavior behavior : behaviors) {
			if (behavior.Allowed_Movement == AllowedMoves.MouseOver) {
				current_behavior = behavior;
				break;
			}
		}
	}
	
	public void draw(Canvas canvas) {
		if(isPonyOnScreen(position)){
			this.current_behavior.draw(canvas, position);
		}
	}
	
	public boolean isPonyOnLocation(int x, int y){
		Rect ponyBox = new Rect(position.x, position.y, position.x + current_behavior.getCurrentImage().getSpriteWidth(), position.y + current_behavior.getCurrentImage().getSpriteHeight());
		return ponyBox.contains(x, y);
	}
	
	/**
	 * Checks if location is on the wallpaper
	 * @param location
	 * @return
	 */
	public boolean isPonyOnWallpaper(Point location) {
		List<Point> points = new LinkedList<Point>();
		points.add(location);
		points.add(new Point(location.x + this.current_behavior.getCurrentImage().getSpriteWidth(), location.y + this.current_behavior.getCurrentImage().getSpriteHeight()));
		points.add(new Point(location.x + this.current_behavior.getCurrentImage().getSpriteWidth(), location.y));
		points.add(new Point(location.x, location.y + this.current_behavior.getCurrentImage().getSpriteHeight()));

		for (Point point : points) {				
			if (RenderEngine.screenBounds.contains(point.x, point.y) == false) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks if the Pony is currently visible on the screen
	 * @param location Location to check
	 * @return True if visible
	 */
	public boolean isPonyOnScreen(Point location){
		List<Point> points = new LinkedList<Point>();
		points.add(new Point(location.x + RenderEngine.OFFSET, location.y));
		points.add(new Point(location.x + this.current_behavior.getCurrentImage().getSpriteWidth() + RenderEngine.OFFSET, location.y + this.current_behavior.getCurrentImage().getSpriteHeight()));
		points.add(new Point(location.x + this.current_behavior.getCurrentImage().getSpriteWidth() + RenderEngine.OFFSET, location.y));
		points.add(new Point(location.x + RenderEngine.OFFSET, location.y + this.current_behavior.getCurrentImage().getSpriteHeight()));

		for (Point point : points) {
			if (RenderEngine.visibleScreenArea.contains(point.x, point.y) == true) {
				return true;
			}
		}
		return false;
	}
	
	public void addBehavior(String name, double chance, double max_duration,  double min_duration,  double speed,
            String right_image_path, String left_image_path, AllowedMoves Allowed_Moves,
            String _Linked_Behavior, boolean _skip,
            int _xcoord, int _ycoord) throws IOException {
		
		// Create a new behavior structure
		Behavior new_behavior = new Behavior();
		
		// Set its values
       new_behavior.name = name.trim();
       new_behavior.chance = chance;
       new_behavior.maxDuration = max_duration;
       new_behavior.minDuration = min_duration;
       new_behavior.speed = speed;
       new_behavior.Allowed_Movement = Allowed_Moves;

       new_behavior.Skip = _skip;
       
       new_behavior.destination_xcoord = _xcoord;
       new_behavior.destination_ycoord = _ycoord; 
       
       
       if (_Linked_Behavior != null && _Linked_Behavior.length() > 0) {
    	   new_behavior.linkedBehaviorName = _Linked_Behavior;
       }
		
       new_behavior.image_right_path = right_image_path;
       new_behavior.image_left_path = left_image_path;
			
       // Add this new behavior to the list
       behaviors.add(new_behavior);			
		       
	}
	
	public Behavior getAppropriateBehavior(AllowedMoves movement, boolean speed, Behavior specified_behavior) {
		int selected_behavior_speed = 0;
		Behavior selected_behavior = current_behavior;

		//does the current behavior work?  /use current behavior images when following
		if ((movement == AllowedMoves.None && (current_behavior.Allowed_Movement == AllowedMoves.None || current_behavior.Allowed_Movement == AllowedMoves.MouseOver)) ||
				(movement == AllowedMoves.Diagonal_Only && (current_behavior.Allowed_Movement == AllowedMoves.Diagonal_Horizontal || current_behavior.Allowed_Movement == AllowedMoves.Diagonal_Only || current_behavior.Allowed_Movement == AllowedMoves.Diagonal_Vertical)) ||
				(movement == AllowedMoves.Vertical_Only && (current_behavior.Allowed_Movement == AllowedMoves.Diagonal_Vertical || current_behavior.Allowed_Movement == AllowedMoves.Horizontal_Vertical || current_behavior.Allowed_Movement == AllowedMoves.Vertical_Only)) ||
				(movement == AllowedMoves.Horizontal_Only && (current_behavior.Allowed_Movement == AllowedMoves.Diagonal_Horizontal || current_behavior.Allowed_Movement == AllowedMoves.Horizontal_Vertical || current_behavior.Allowed_Movement == AllowedMoves.Horizontal_Only)) ||
				(movement == current_behavior.Allowed_Movement) ||
				(movement == AllowedMoves.All)
				) {
			if (current_behavior.speed == 0 && movement == AllowedMoves.None)
				return current_behavior;
			
			if (current_behavior.speed != 0 && movement == AllowedMoves.All)
				return current_behavior;
		}
		
		// Loop through the list of behaviors
		for (Behavior behavior : behaviors) {
			if ((movement == AllowedMoves.None && (behavior.Allowed_Movement == AllowedMoves.None || behavior.Allowed_Movement == AllowedMoves.MouseOver)) ||
				(movement == AllowedMoves.Diagonal_Only && (behavior.Allowed_Movement == AllowedMoves.Diagonal_Horizontal || behavior.Allowed_Movement == AllowedMoves.Diagonal_Only || behavior.Allowed_Movement == AllowedMoves.Diagonal_Vertical)) ||
				(movement == AllowedMoves.Vertical_Only && (behavior.Allowed_Movement == AllowedMoves.Diagonal_Vertical || behavior.Allowed_Movement == AllowedMoves.Horizontal_Vertical || behavior.Allowed_Movement == AllowedMoves.Vertical_Only)) ||
				(movement == AllowedMoves.Horizontal_Only && (behavior.Allowed_Movement == AllowedMoves.Diagonal_Horizontal || behavior.Allowed_Movement == AllowedMoves.Horizontal_Vertical || behavior.Allowed_Movement == AllowedMoves.Horizontal_Only)) ||
				(movement == behavior.Allowed_Movement) ||
				(movement == AllowedMoves.All)
				) {
				if (behavior.Skip == false) {
					if (behavior.speed == 0 && movement != AllowedMoves.All) {
						if (ponyDirection == true)
							behavior.right = true;
						else
							behavior.right = false;
						
						return behavior;
					} else {
						// see if the specified behavior works. If not, we'll find another.
						if (specified_behavior != null) {
							if ((movement == AllowedMoves.None && (specified_behavior.Allowed_Movement == AllowedMoves.None || specified_behavior.Allowed_Movement == AllowedMoves.MouseOver)) ||
								(movement == AllowedMoves.Diagonal_Only && (specified_behavior.Allowed_Movement == AllowedMoves.Diagonal_Horizontal || specified_behavior.Allowed_Movement == AllowedMoves.Diagonal_Only || specified_behavior.Allowed_Movement == AllowedMoves.Diagonal_Vertical)) ||
								(movement == AllowedMoves.Vertical_Only && (specified_behavior.Allowed_Movement == AllowedMoves.Diagonal_Vertical || specified_behavior.Allowed_Movement == AllowedMoves.Horizontal_Vertical || specified_behavior.Allowed_Movement == AllowedMoves.Vertical_Only)) ||
								(movement == AllowedMoves.Horizontal_Only && (specified_behavior.Allowed_Movement == AllowedMoves.Diagonal_Horizontal || specified_behavior.Allowed_Movement == AllowedMoves.Horizontal_Vertical || specified_behavior.Allowed_Movement == AllowedMoves.Horizontal_Only)) ||
								(movement == specified_behavior.Allowed_Movement) ||
								(movement == AllowedMoves.All)
								) {
								if (destination.x == 0 && destination.y == 0) {
									if (ponyDirection == true)
										specified_behavior.right = true;
									else
										specified_behavior.right = false;
								} else {
									if (Get_Destination_Direction(destination).get(0) == Directions.right)
										specified_behavior.right = true;
									else
										specified_behavior.right = false;
								}
								
								return specified_behavior;
							}
						}
						
						if (speed) {
							// Find the behavior with the greatest speed
							if (Math.abs(behavior.speed) > selected_behavior_speed) {
								selected_behavior = behavior;
								selected_behavior_speed = (int) Math.abs(behavior.speed);
							}
						} else {
							// Otherwise find the behavior with the smallest speed
							if (Math.abs(behavior.speed) < selected_behavior_speed || selected_behavior_speed == 0) {
								selected_behavior = behavior;
								selected_behavior_speed = (int) Math.abs(behavior.speed);
							}
						}
					}
				}
			}
		}
		
		// Return the behavior we found
		return selected_behavior;
	}
	
	private List<Directions> Get_Destination_Direction(Point destination) {
		List<Directions> direction = new LinkedList<Directions>();
		
		// Do we need to go left or right?
		if (destination.x - position.x <= 0)
			direction.add(Directions.left);
		else
			direction.add(Directions.right);
		
		// Do we need to go up or down?
		if (destination.y - position.y <= 0)
			direction.add(Directions.top);
		else
			direction.add(Directions.bottom);
		
		return direction;
	}
	
	public void selectBehavior(Behavior Specified_Behavior, long globalTime) {
		//if (Is_Interacting && Specified_Behavior == null) Cancel_Interaction();
		long startTime = SystemClock.elapsedRealtime();
						
		previous_behavior = current_behavior;
		
		if(MyLittleWallpaperService.DEBUG_RENDERTIME) Log.i("Pony[" + name + "]", "Picking from " + behaviors.size());

		double dice;
		
		int selection = 0;
		
		// Are we being forced into a specific behavior or not?
		if (Specified_Behavior == null) {
			int loop_total = 0;
			
			// Randomly select a non-skip behavior
			while(loop_total <= 200) {
				dice = MyLittleWallpaperService.rand.nextDouble();
				
				selection = MyLittleWallpaperService.rand.nextInt(behaviors.size());
				if (dice <= behaviors.get(selection).chance && behaviors.get(selection).Skip == false) {
		
	                destination = behaviors.get(selection).getDestination(RenderEngine.screenBounds.width(), RenderEngine.screenBounds.height());
	
	                //if (!(destination.x == 0 && destination.y == 0)) {
						current_behavior = behaviors.get(selection);
						break;
	                //}
				}
				loop_total++;
			}
			
			if (loop_total > 200) {
				// If the Random number generator is being goofy, select the default behavior (usually standing)
				current_behavior = behaviors.get(0);
				if(MyLittleWallpaperService.DEBUG_RENDERTIME) Log.i("Pony[" + name + "]", "forced to 0");
			}				
		} else { // Set the forced behavior that was specified
			destination = Specified_Behavior.getDestination(RenderEngine.screenBounds.width(), RenderEngine.screenBounds.height());

			if (destination.x == 0 && destination.y == 0) {
				selectBehavior(null, globalTime);
				return;
			}
			current_behavior = Specified_Behavior;
		}
		
		
		// Set the time this behavior will last
        current_behavior.endTime = (globalTime + Math.round(MyLittleWallpaperService.rand.nextDouble() * (current_behavior.maxDuration - current_behavior.minDuration) * 1000 + (current_behavior.minDuration * 1000)));
	    
		// Select facing (left or right)
		dice = MyLittleWallpaperService.rand.nextDouble();
		if (dice >= 0.5)
			current_behavior.right = true;
		else
			current_behavior.right = false;
			
	    // If we aren't moving anywhere, stop here
	    if ((current_behavior.Allowed_Movement == AllowedMoves.None) || (current_behavior.Allowed_Movement == AllowedMoves.MouseOver) ||
	    		current_behavior.Allowed_Movement == AllowedMoves.Sleep) {
	        current_behavior.horizontal = false;
	        current_behavior.vertical = false;
	        return;
	    }
	    
	    // Otherwise, randomly select the movement direction based on where we're allowed to move
	    List<AllowedMoves> modes = new LinkedList<AllowedMoves>();
	    if (current_behavior.Allowed_Movement == AllowedMoves.All ||
	    		current_behavior.Allowed_Movement == AllowedMoves.Diagonal_Vertical ||
	    		current_behavior.Allowed_Movement == AllowedMoves.Horizontal_Vertical ||
	    		current_behavior.Allowed_Movement == AllowedMoves.Vertical_Only) {
	    	modes.add(AllowedMoves.Vertical_Only);
	    }
	    if (current_behavior.Allowed_Movement == AllowedMoves.All ||
	    		current_behavior.Allowed_Movement == AllowedMoves.Diagonal_Vertical ||
	    		current_behavior.Allowed_Movement == AllowedMoves.Diagonal_Horizontal ||
	    		current_behavior.Allowed_Movement == AllowedMoves.Diagonal_Only) {
	    	modes.add(AllowedMoves.Diagonal_Only);
	    }
	    if (current_behavior.Allowed_Movement == AllowedMoves.All ||
	    		current_behavior.Allowed_Movement == AllowedMoves.Diagonal_Horizontal ||
	    		current_behavior.Allowed_Movement == AllowedMoves.Horizontal_Only ||
	    		current_behavior.Allowed_Movement == AllowedMoves.Horizontal_Vertical) {
	    	modes.add(AllowedMoves.Horizontal_Only);
	    }
	    
	    if (modes.size() == 0) {
	    	System.out.println("Unhandled movement type in SelectBehavior()");
	    	return;
	    }
	    
	    selection = MyLittleWallpaperService.rand.nextInt(modes.size());
	    AllowedMoves selected_mode = modes.get(selection);
	    
	    switch(selected_mode) {
	    	case Vertical_Only:
	    		current_behavior.horizontal = false;
	    		current_behavior.vertical = true;
	    		break;
	    	case Diagonal_Only:
	    		current_behavior.horizontal = true;
	    		current_behavior.vertical = true;
	    		break;
	    	case Horizontal_Only:
	    		current_behavior.horizontal = true;
	    		current_behavior.vertical = false;
	    		break;
	    }
	    
	    dice = MyLittleWallpaperService.rand.nextDouble();
	    
	    if (dice >= 0.5)
	    	current_behavior.up = true;
	    else
	    	current_behavior.up = false;
	    
	    dice = MyLittleWallpaperService.rand.nextDouble();
	    
	    if (dice >= 0.5)
	    	current_behavior.right = true;
	    else
	    	current_behavior.right = false;
	    
	    long timeNeeded = SystemClock.elapsedRealtime() - startTime;
	    
	    // TODO Tell the GC to pick up the old behavior
		if(current_behavior != null && previous_behavior != null && (previous_behavior.equals(current_behavior) == false)){
			Log.i("Pony[" + name + "]", "swaping from " + previous_behavior.name + " to " + current_behavior.name);
			previous_behavior.destroy();
			previous_behavior = null;
			//System.gc();
		}
	    
		if(MyLittleWallpaperService.DEBUG_RENDERTIME) Log.i("Pony[" + name + "]", "Found new Behavior after " + timeNeeded + " ms. Using \"" + current_behavior.name + "\" for " + Math.round((current_behavior.endTime - SystemClock.elapsedRealtime()) / 1000) + " sec");
	}
	
	public void setDestination(int x, int y){
		this.destination = new Point(x, y);
		getAppropriateBehavior(AllowedMoves.All, true, null);
	}
	
	public void linkBehaviors(){
		for(Behavior b : behaviors){
			if(b.linkedBehaviorName == null)
				continue;
			
			for(Behavior links : behaviors){
				if(links.name.equals(b.linkedBehaviorName)){
					b.linkedBehavior = links;
					break;
				}
			}
		}
	}	
}
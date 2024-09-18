public class Player {
	private String name;
	private int xpos;
	private int ypos;
	private String direction;
	private int score;

	public Player(String name, int xpos, int ypos, String direction) {
		this.name = name;
		this.xpos = xpos;
		this.ypos = ypos;
		this.direction = direction;
		this.score = 0;
	}

	public String getName() { return name; }
	public int getXpos() { return xpos; }
	public int getYpos() { return ypos; }
	public String getDirection() { return direction; }
	public int getScore() { return score; }

	public void setName(String name) { this.name = name; }
	public void setXpos(int xpos) { this.xpos = xpos; }
	public void setYpos(int ypos) { this.ypos = ypos; }
	public void setDirection(String direction) { this.direction = direction; }
	public void addPoints(int points) { this.score += points; }

	public void move(int delta_x, int delta_y, String direction) {
		this.xpos += delta_x;
		this.ypos += delta_y;
		this.direction = direction;
	}

	@Override
	public String toString() {
		return name + ": " + score;
	}
}
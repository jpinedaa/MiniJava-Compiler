package generators;

import java.util.LinkedList;
import java.util.Queue;

public class Generator {
	private int count;
	private String prefix;
	private Queue<Integer> toReuse;
	
	public Generator(String prefix) {
		this.prefix = prefix;
		this.count = 0;
		this.toReuse = new LinkedList<Integer>();
	}
	
	public String next() {
		int num = (this.toReuse.isEmpty()) ? this.count++ : this.toReuse.remove();
		return prefix + Integer.toString(num);
	}
	
	public void reset(int count) {
		this.count = count;
	}
	
	public void reuse(int num) {
		this.toReuse.add(num);
	}

}

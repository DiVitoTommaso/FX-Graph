package graph.dataclasses;

public class FlowWeight {

	public int value;
	public int capacity;

	public FlowWeight(int value, int capacity) {
		super();
		this.value = value;
		this.capacity = capacity;
	}

	public int getAvailable() {
		return capacity - value;
	}

	@Override
	public String toString() {
		return "[" + value + "," + capacity + "]";
	}

}

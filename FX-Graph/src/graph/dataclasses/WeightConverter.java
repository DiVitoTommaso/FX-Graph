package graph.dataclasses;

import graph.annotations.Nullable;

public interface WeightConverter<K> {

	public double convert(@Nullable K val);
}

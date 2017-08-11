package src.dualSimulation.core.utility;

/**
 * Created by shayan on 6/7/16.
 */
public class Tuple2<P, I> {
	public P _1;
	public I _2;

	public Tuple2(P _1, I _2) {
		this._1 = _1;
		this._2 = _2;
	}

	@Override
	public String toString() {
		return "(" + _1 + ", " + _2 + ')';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Tuple2<?, ?> tuple2 = (Tuple2<?, ?>) o;

		if (!_1.equals(tuple2._1))
			return false;
		return _2.equals(tuple2._2);

	}

	@Override
	public int hashCode() {
		int result = _1.hashCode();
		result = 31 * result + _2.hashCode();
		return result;
	}

}

package graph.gui;

import graph.annotations.NotNull;
import graph.annotations.Nullable;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

public class Edge<K> extends Group {

	private final Line line;
	private final ObjectProperty<K> weight;

	/**
	 * 
	 * Hashable object.This object can be used as key in dictionaries, even if value
	 * is not immutable because the hashcode will be generated only on creation.
	 * 
	 * create a graphic edge
	 * 
	 * @param xs     x start
	 * @param ys     y start
	 * @param xe     x end
	 * @param ye     y end
	 * @param radius radius offset
	 */

	public Edge(@NotNull Node<?> from, @NotNull Node<?> to, @NotNull double radius, @Nullable K weight) {
		this(new Line(), new Line(), new Line(), new Text(), new SimpleObjectProperty<K>(weight), radius);
		setStartX(from.getLayoutX() + from.getPrefWidth() / 2);
		setStartY(from.getLayoutY() + from.getPrefHeight() / 2);
		setEndX(to.getLayoutX() + to.getPrefWidth() / 2);
		setEndY(to.getLayoutY() + to.getPrefHeight() / 2);
	}

	/**
	 * Hashable object.This object can be used as key in dictionaries, even if value
	 * is not immutable because the hashcode will be generated only on creation.
	 * create a graphic edge
	 * 
	 * @param xs     x start
	 * @param ys     y start
	 * @param xe     x end
	 * @param ye     y end
	 * @param radius radius offset
	 * @param weight initial arrow weight
	 */

	public Edge(@NotNull Node<?> from, @NotNull Node<?> to, @NotNull double radius) {
		this(from, to, radius, null);
	}

	private static final double arrowLength = 20;
	private static final double arrowWidth = 15;

	private Edge(Line line, Line arrow1, Line arrow2, Text text, ObjectProperty<K> weight, double radius) {
		super(line, arrow1, arrow2, text);

		// check if constructor call was made by javafx thread
		if (!Platform.isFxApplicationThread())
			throw new IllegalStateException("Not on JavaFX application thread");

		this.weight = weight;
		this.line = line;

		// init weight
		if (weight.get() != null)
			text.setText(weight.getValue().toString());

		if (text.getText().endsWith(".0"))
			text.setText(weight.getValue().toString().replace(".0", ""));

		text.setStyle("-fx-font-size: 28; -fx-font-weight: bold;");
		text.setFill(Color.GREEN);

		InvalidationListener updater = o -> {

			// check if change was made by javafx thread or not
			if (!Platform.isFxApplicationThread())
				throw new IllegalStateException("Not on JavaFX application thread");

			double ex = getEndX();
			double ey = getEndY();
			double sx = getStartX();
			double sy = getStartY();

			arrow1.setEndX(ex);
			arrow1.setEndY(ey);
			arrow2.setEndX(ex);
			arrow2.setEndY(ey);

			if (Math.abs(ex - sx) < 20 && Math.abs(ey - sy) < 20) {
				arrow1.setStartX(ex);
				arrow1.setStartY(ey);
				arrow2.setStartX(ex);
				arrow2.setStartY(ey);
			} else {
				double factor = arrowLength / Math.hypot(sx - ex, sy - ey);
				double factorO = arrowWidth / Math.hypot(sx - ex, sy - ey);

				double dx = (sx - ex) * factor;
				double dy = (sy - ey) * factor;

				double ox = (sx - ex) * factorO;
				double oy = (sy - ey) * factorO;

				arrow1.setStartX(ex + dx - oy);
				arrow1.setStartY(ey + dy + ox);
				arrow2.setStartX(ex + dx + oy);
				arrow2.setStartY(ey + dy - ox);

				text.setLayoutX((ex + sx) / 2 - radius * Math.cos(Math.atan2(ey - sy, ex - sx)));
				text.setLayoutY((ey + sy) / 2 - radius * Math.sin(Math.atan2(ey - sy, ex - sx)));

				arrow1.setLayoutX(-radius * Math.cos(Math.atan2(ey - sy, ex - sx)));
				arrow2.setLayoutX(-radius * Math.cos(Math.atan2(ey - sy, ex - sx)));
				arrow1.setLayoutY(-radius * Math.sin(Math.atan2(ey - sy, ex - sx)));
				arrow2.setLayoutY(-radius * Math.sin(Math.atan2(ey - sy, ex - sx)));

				arrow1.setStrokeWidth(line.getStrokeWidth());
				arrow2.setStrokeWidth(line.getStrokeWidth());

				arrow1.setStroke(line.getStroke());
				arrow2.setStroke(line.getStroke());

				if (weight.get() != null)
					text.setText(weight.getValue().toString());

				if (text.getText().endsWith(".0"))
					text.setText(weight.getValue().toString().replace(".0", ""));

				text.toFront();
			}
		};

		// listen for changes
		startXProperty().addListener(updater);
		startYProperty().addListener(updater);
		endXProperty().addListener(updater);
		endYProperty().addListener(updater);
		strokeWidthProperty().addListener(updater);
		weightProperty().addListener(updater);
		updater.invalidated(null);
	}

	public final void setStartX(double value) {
		line.setStartX(value);
	}

	public final double getStartX() {
		return line.getStartX();
	}

	public final DoubleProperty strokeWidthProperty() {
		return line.strokeWidthProperty();
	}

	public final DoubleProperty startXProperty() {
		return line.startXProperty();
	}

	public final void setStartY(double value) {
		line.setStartY(value);
	}

	public final double getStartY() {
		return line.getStartY();
	}

	public final DoubleProperty startYProperty() {
		return line.startYProperty();
	}

	public final void setEndX(double value) {
		line.setEndX(value);
	}

	public final double getEndX() {
		return line.getEndX();
	}

	public final DoubleProperty endXProperty() {
		return line.endXProperty();
	}

	public final void setEndY(double value) {
		line.setEndY(value);
	}

	public final double getEndY() {
		return line.getEndY();
	}

	public final DoubleProperty endYProperty() {
		return line.endYProperty();
	}

	public final void setStrokeWidth(double value) {
		line.setStrokeWidth(value);
	}

	public final K getWeight() {
		return weight.get();
	}

	public final void setWeight(K w) {
		weight.set(w);
	}

	public final ObjectProperty<K> weightProperty() {
		return weight;
	}

	public final void setStroke(Color c) {
		line.setStroke(c);
	}

	public final ObjectProperty<Paint> strokeProperty() {
		return line.strokeProperty();
	}

}

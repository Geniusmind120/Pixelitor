/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.filters.gui;

import com.jhlabs.image.ImageMath;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.utils.Rnd;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.Rectangle;
import java.util.function.BooleanSupplier;

import static java.lang.String.format;
import static pixelitor.filters.gui.RandomizePolicy.ALLOW_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

/**
 * Represents an integer value with a minimum, a maximum and a default.
 * Suitable as the model of a JSlider (but usually used as a model of
 * an entire SliderSpinner)
 */
public class RangeParam extends AbstractFilterParam implements BoundedRangeModel {
    private int minValue;
    private int maxValue;
    private double defaultValue;
    private int decimalPlaces = 0;

     // Not stored as an int in order to enable animation interpolations
    private double value;

    private boolean adjusting;
    private final boolean addDefaultButton;
    private final SliderSpinner.TextPosition textPosition;

    private ChangeEvent changeEvent = null;
    private final EventListenerList listenerList = new EventListenerList();
    private boolean adjustMaxAccordingToImage = false;
    private double maxToImageSizeRatio;

    public RangeParam(String name, int min, double def, int max) {
        this(name, min, def, max, true, BORDER);
    }

    public RangeParam(String name, int min, double def, int max, boolean addDefaultButton,
                      SliderSpinner.TextPosition position) {
        this(name, min, def, max, addDefaultButton, position, ALLOW_RANDOMIZE);
    }

    public RangeParam(String name, int min, double def, int max, boolean addDefaultButton,
                      SliderSpinner.TextPosition position, RandomizePolicy randomizePolicy) {
        super(name, randomizePolicy);

        assert min < max : name + ": min (" + min + ") >= max (" + max + ')';
        assert def >= min : name + ": def (" + def + ") < min (" + min + ')';
        assert def <= max : name + ": def (" + def + ") > max (" + max + ')';

        minValue = min;
        maxValue = max;
        defaultValue = def;
        value = def;
        this.addDefaultButton = addDefaultButton;
        textPosition = position;
    }

    @Override
    public JComponent createGUI() {
        var sliderSpinner = new SliderSpinner(this, textPosition, addDefaultButton);
        paramGUI = sliderSpinner;
        setGUIEnabledState();

        if (action != null) {
            return new ParamGUIWithAction(sliderSpinner, action);
        }

        return sliderSpinner;
    }

    /**
     * Sets up the automatic enabling of another {@link FilterSetting}
     * when the value of this one is not zero.
     * Typically used when this is a randomness slider, and the other
     * is a "reseed randomness" button.
     */
    public void setupEnableOtherIfNotZero(FilterSetting other) {
        other.setEnabled(getValue() != 0, EnabledReason.APP_LOGIC);
        addChangeListener(e ->
                other.setEnabled(getValue() != 0,
                        EnabledReason.APP_LOGIC));
    }

    /**
     * Makes sure that this {@link RangeParam} always has a higher
     * or equal value than the given other {@link RangeParam}
     */
    public void ensureHigherValueThan(RangeParam other) {
        // if the value is not higher, then make it equal
        linkWith(other, () -> other.getValue() > getValue());
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(int dp) {
        decimalPlaces = dp;
    }

    public RangeParam withDecimalPlaces(int dp) {
        setDecimalPlaces(dp);
        return this;
    }

    /**
     * Synchronizes the value of this object with the value of another
     * {@link RangeParam} if the given condition evaluates to true.
     */
    public void linkWith(RangeParam other, BooleanSupplier condition) {
        addChangeListener(e -> {
            if (condition.getAsBoolean()) {
                other.setValueNoTrigger(getValueAsDouble());
            }
        });
        other.addChangeListener(e -> {
            if (condition.getAsBoolean()) {
                setValueNoTrigger(other.getValueAsDouble());
            }
        });
    }

    /**
     * Synchronizes the value of this object with the value of another
     * {@link RangeParam} so that there is a constant multiplier between the values.
     */
    public void linkWith(RangeParam other, double multiplier) {
        addChangeListener(e -> other.setValueNoTrigger(
                getValueAsDouble() * multiplier));
        other.addChangeListener(e -> setValueNoTrigger(
                other.getValueAsDouble() / multiplier));
    }

    @Override
    public boolean isSetToDefault() {
        return Math.abs(getValueAsDouble() - defaultValue) < 0.005;
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    /**
     * Resets to the default value.
     *
     * @param trigger should be true if called from a GUI component
     */
    @Override
    public void reset(boolean trigger) {
        setValue(defaultValue, trigger);
    }

    /**
     * Returns the value of a percentage parameter as a float ratio
     */
    public float getPercentageValF() {
        return getValueAsFloat() / 100.0f;
    }

    /**
     * Returns the value of a percentage parameter as a double ratio
     */
    public double getPercentageValD() {
        return getValueAsDouble() / 100.0;
    }

    /**
     * Int values measured in degrees are transformed to radians
     */
    public float getValueInRadians() {
        return (float) Math.toRadians(getValueAsDouble());
    }

    @Override
    protected void doRandomize() {
        int range = maxValue - minValue;
        int newValue = minValue + Rnd.nextInt(range);

        setValueNoTrigger(newValue);
    }

    public void increaseValue() {
        int intValue = (int) value;
        if (intValue < maxValue) {
            setValue(intValue + 1);
        }
    }

    public void decreaseValue() {
        int intValue = (int) value;
        if (intValue > minValue) {
            setValue(intValue - 1);
        }
    }

    @Override
    public int getMinimum() {
        return minValue;
    }

    @Override
    public void setMinimum(int newMinimum) {
        minValue = newMinimum;
    }

    @Override
    public int getMaximum() {
        return maxValue;
    }

    @Override
    public void setMaximum(int newMaximum) {
        maxValue = newMaximum;
    }

    @Override
    public int getValue() {
        return (int) value;
    }

    public boolean isZero() {
        return getValue() == 0;
    }

    public float getValueAsFloat() {
        return (float) value;
    }

    public double getValueAsDouble() {
        return value;
    }

    // accepts an int so that the class can implement BoundedRangeModel
    @Override
    public void setValue(int n) {
        setValue(n, true);
    }

    public void setValueNoTrigger(double n) {
        setValue(n, false);
    }

    public void setValue(double v, boolean trigger) {
        if (v > maxValue) {
            v = maxValue;
        }
        if (v < minValue) {
            v = minValue;
        }

        if (Math.abs(v - value) > 0.001) { // there are max 2 decimal places in the GUI
            value = v;
            fireStateChanged(); // update the GUI
            if (!adjusting && trigger && adjustmentListener != null) {
                adjustmentListener.paramAdjusted(); // run the filter
            }
        }
    }

    /**
     * This is only used programmatically while tweening, therefore
     * it never triggers the filter or the GUI
     */
    public void setValueNoGUI(double d) {
        value = d;
    }

    @Override
    public void setValueIsAdjusting(boolean b) {
        if (!b) {
            if (adjusting) {
                if (adjustmentListener != null) {
                    adjustmentListener.paramAdjusted();
                }
            }
        }
        if (adjusting != b) {
            adjusting = b;
            fireStateChanged();
        }
    }

    @Override
    public boolean getValueIsAdjusting() {
        return adjusting;
    }

    @Override
    public int getExtent() {
        return 0;
    }

    @Override
    public void setExtent(int newExtent) {
        // not used
    }

    @Override
    public void setRangeProperties(int value, int extent, int min, int max, boolean adjusting) {
        // not used
    }

    @Override
    public void addChangeListener(ChangeListener x) {
        assert x != null;
        listenerList.add(ChangeListener.class, x);
    }

    @Override
    public void removeChangeListener(ChangeListener x) {
        listenerList.remove(ChangeListener.class, x);
    }

    private void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ChangeListener listener = (ChangeListener) listeners[i + 1];
                listener.stateChanged(changeEvent);
            }
        }
    }

    @Override
    public void considerImageSize(Rectangle bounds) {
        if (adjustMaxAccordingToImage) {
            double defaultToMaxRatio = defaultValue / maxValue;
            maxValue = (int) (maxToImageSizeRatio * Math.max(bounds.width, bounds.height));
            if (maxValue <= minValue) { // can happen with very small (for example 1x1) images
                maxValue = minValue + 1;
            }

            // make sure that the tic/label for max value is painted, see issue #91
            maxValue += (4 - (maxValue - minValue) % 4);

            defaultValue = (int) (defaultToMaxRatio * maxValue);
            if (defaultValue > maxValue) {
                defaultValue = maxValue;
            }
            if (defaultValue < minValue) {
                defaultValue = minValue;
            }
            value = defaultValue;
        }
    }

    public RangeParam withAdjustedRange(double ratio) {
        maxToImageSizeRatio = ratio;
        adjustMaxAccordingToImage = true;
        return this;
    }

    @Override
    public boolean canBeAnimated() {
        return true;
    }

    @Override
    public RangeParamState copyState() {
        return new RangeParamState(value);
    }

    @Override
    public void setState(ParamState<?> state) {
        value = ((RangeParamState) state).getValue();
    }

    @Override
    public String getResetToolTip() {
        String defaultAsString;
        if (decimalPlaces == 0) {
            defaultAsString = String.valueOf((int) defaultValue);
        } else if (decimalPlaces == 1) {
            defaultAsString = format("%.1f", defaultValue);
        } else if (decimalPlaces == 2) {
            defaultAsString = format("%.2f", defaultValue);
        } else {
            throw new IllegalStateException();
        }
        return super.getResetToolTip() + " to " + defaultAsString;
    }

    @Override
    public Object getParamValue() {
        return value;
    }

    public RangeParam copy() {
        return new RangeParam(getName(), minValue, value, maxValue, addDefaultButton, textPosition, randomizePolicy);
    }

    @Override
    public String toString() {
        return format("%s[name = '%s', value = %.2f]",
                getClass().getSimpleName(), getName(), value);
    }

    public static class Builder {
        private final String name;
        private int min;
        private double def;
        private int max;
        private boolean addDefaultButton = true;
        private int decimalPlaces = 0;
        private SliderSpinner.TextPosition textPosition = BORDER;
        private RandomizePolicy randomizePolicy = ALLOW_RANDOMIZE;

        public Builder(String name) {
            this.name = name;
        }

        public Builder min(int min) {
            this.min = min;
            return this;
        }

        public Builder def(double def) {
            this.def = def;
            return this;
        }

        public Builder max(int max) {
            this.max = max;
            return this;
        }

        public Builder withDecimalPlaces(int dp) {
            decimalPlaces = dp;
            return this;
        }

        public Builder addDefaultButton(boolean addDefaultButton) {
            this.addDefaultButton = addDefaultButton;
            return this;
        }

        public Builder textPosition(SliderSpinner.TextPosition textPosition) {
            this.textPosition = textPosition;
            return this;
        }

        public Builder randomizePolicy(RandomizePolicy randomizePolicy) {
            this.randomizePolicy = randomizePolicy;
            return this;
        }

        public RangeParam build() {
            RangeParam rp = new RangeParam(name, min, def, max,
                    addDefaultButton, textPosition, randomizePolicy);
            rp.setDecimalPlaces(decimalPlaces);
            return rp;
        }
    }

    private static class RangeParamState implements ParamState<RangeParamState> {
        final double value;

        public RangeParamState(double value) {
            this.value = value;
        }

        @Override
        public RangeParamState interpolate(RangeParamState endState, double progress) {
            double interpolated = ImageMath.lerp(progress, value, endState.value);
            return new RangeParamState(interpolated);
        }

        public double getValue() {
            return value;
        }

        @Override
        public String toString() {
            return format("%s[value=%.2f]",
                    getClass().getSimpleName(), value);
        }
    }
}

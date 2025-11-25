import { useState } from 'react';
import './Slider.css';

interface SliderProps {
  label: string;
  value: number;
  min: number;
  max: number;
  step?: number;
  unit: string;
  onChange: (value: number) => void;
  description?: string;
}

const Slider = ({ 
  label, 
  value, 
  min, 
  max, 
  step = 1, 
  unit, 
  onChange,
  description 
}: SliderProps) => {
  const percentage = ((value - min) / (max - min)) * 100;

  return (
    <div className="slider-container">
      <div className="slider-header">
        <label className="slider-label">{label}</label>
        <span className="slider-value">{value} {unit}</span>
      </div>
      
      <div className="slider-track-container">
        <div className="slider-track">
          <div 
            className="slider-progress" 
            style={{ width: `${percentage}%` }}
          />
          <input
            type="range"
            className="slider-input"
            min={min}
            max={max}
            step={step}
            value={value}
            onChange={(e) => onChange(Number(e.target.value))}
          />
        </div>
      </div>

      <div className="slider-labels">
        <span>{min}</span>
        <span>{Math.floor((min + max) / 2)}</span>
        <span>{max}</span>
      </div>

      {description && (
        <p className="slider-description">{description}</p>
      )}
    </div>
  );
};

export default Slider;


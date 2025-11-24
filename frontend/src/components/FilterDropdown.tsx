import { useState } from 'react';
import './FilterDropdown.css';

interface FilterDropdownProps {
  value: string;
  options: string[];
  onChange: (value: string) => void;
}

const FilterDropdown = ({ value, options, onChange }: FilterDropdownProps) => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className="filter-dropdown">
      <button className="filter-button" onClick={() => setIsOpen(!isOpen)}>
        <span>{value}</span>
        <span className="dropdown-icon">▼</span>
      </button>
      {isOpen && (
        <div className="filter-options">
          {options.map((option) => (
            <div
              key={option}
              className={`filter-option ${value === option ? 'active' : ''}`}
              onClick={() => {
                onChange(option);
                setIsOpen(false);
              }}
            >
              {option}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default FilterDropdown;


import './StatCard.css';

interface StatCardProps {
  title: string;
  value: number | string;
  trend: string;
  icon: string;
  iconColor: string;
}

const StatCard = ({ title, value, trend, icon, iconColor }: StatCardProps) => {
  return (
    <div className="stat-card">
      <div className="stat-icon" style={{ backgroundColor: iconColor }}>
        {icon}
      </div>
      <div className="stat-content">
        <p className="stat-title">{title}</p>
        <h3 className="stat-value">{value}</h3>
        <div className="stat-trend">
          <span className="trend-icon">↗</span>
          <span className="trend-text">{trend}</span>
        </div>
      </div>
    </div>
  );
};

export default StatCard;


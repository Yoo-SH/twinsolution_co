import './ActionCard.css';

interface ActionCardProps {
  title: string;
  description: string;
  icon: string;
  color: string;
  onClick?: () => void;
}

const ActionCard = ({ title, description, icon, color, onClick }: ActionCardProps) => {
  return (
    <div className="action-card" style={{ backgroundColor: color }} onClick={onClick}>
      <div className="action-icon">{icon}</div>
      <h4 className="action-title">{title}</h4>
      <p className="action-description">{description}</p>
    </div>
  );
};

export default ActionCard;


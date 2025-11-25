import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import './ChartCard.css';

interface ChartCardProps {
  title: string;
}

const ChartCard = ({ title }: ChartCardProps) => {
  const data = [
    { name: '1월', value: 30 },
    { name: '2월', value: 45 },
    { name: '3월', value: 35 },
    { name: '4월', value: 60 },
    { name: '5월', value: 50 },
    { name: '6월', value: 75 },
  ];

  return (
    <div className="chart-card">
      <h3 className="chart-title">{title}</h3>
      <div className="chart-container">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />
            <XAxis dataKey="name" stroke="#5a5a5a" />
            <YAxis stroke="#5a5a5a" />
            <Tooltip />
            <Line type="monotone" dataKey="value" stroke="#415a77" strokeWidth={2} />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};

export default ChartCard;


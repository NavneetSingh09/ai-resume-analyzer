import React from 'react';

export default function ScoreBadge({ score }) {
  let className = 'score-badge ';
  if (score >= 60) className += 'score-high';
  else if (score >= 30) className += 'score-mid';
  else className += 'score-low';

  return <span className={className}>{score.toFixed(1)}</span>;
}
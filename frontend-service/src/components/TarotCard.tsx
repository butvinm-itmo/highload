import React from 'react';
import { CardDto } from '../types';

interface TarotCardProps {
  card: CardDto;
  isReversed?: boolean;
  position?: number;
  size?: 'small' | 'medium' | 'large';
}

export function TarotCard({ card, isReversed = false, position, size = 'medium' }: TarotCardProps) {
  const sizeClasses = {
    small: 'w-20 h-32',
    medium: 'w-32 h-48',
    large: 'w-40 h-60',
  };

  const arcanaColor = card.arcanaType.name === 'MAJOR' ? 'border-purple-500' : 'border-blue-500';

  return (
    <div className="flex flex-col items-center">
      <div
        className={`${sizeClasses[size]} bg-white border-2 ${arcanaColor} rounded-lg shadow-md flex flex-col items-center justify-center p-3 ${
          isReversed ? 'transform rotate-180' : ''
        }`}
      >
        <div className={`text-center ${isReversed ? 'transform rotate-180' : ''}`}>
          <div className="text-xs text-gray-500 mb-2">{card.arcanaType.name}</div>
          <div className="text-sm font-semibold text-gray-900 leading-tight">{card.name}</div>
        </div>
      </div>
      {position !== undefined && (
        <div className="mt-2 text-xs text-gray-500">Position {position}</div>
      )}
      {isReversed && (
        <div className="mt-1 text-xs text-red-600 font-medium">Reversed</div>
      )}
    </div>
  );
}

import { useState } from 'react';
import { motion } from 'framer-motion';
import type { CardDto } from '../types';
import { fadeInUp } from '../utils/animations';
import { getCardImagePath, CARD_BACK_IMAGE } from '../config/cardImages';

interface TarotCardProps {
  card: CardDto;
  isReversed?: boolean;
  position?: number;
  size?: 'small' | 'medium' | 'large';
  delay?: number;
  showBack?: boolean;
}

export function TarotCard({ card, isReversed = false, position, size = 'medium', delay = 0, showBack = false }: TarotCardProps) {
  const [imageLoaded, setImageLoaded] = useState(false);
  const [imageError, setImageError] = useState(false);

  const sizeClasses = {
    small: 'w-20 h-32',
    medium: 'w-32 h-48',
    large: 'w-40 h-60',
  };

  const arcanaColor = card.arcanaType.name === 'MAJOR' ? 'border-purple-500' : 'border-blue-500';
  const glowColor = card.arcanaType.name === 'MAJOR' ? 'hover:shadow-purple-500/50' : 'hover:shadow-blue-500/50';

  const imagePath = showBack ? CARD_BACK_IMAGE : getCardImagePath(card.id);
  const shouldShowImage = imagePath && !imageError;

  return (
    <motion.div
      className="flex flex-col items-center"
      variants={fadeInUp}
      initial="hidden"
      animate="visible"
      transition={{ delay }}
    >
      <motion.div
        className={`${sizeClasses[size]} bg-white dark:bg-gray-800 border-2 ${arcanaColor} rounded-lg shadow-md dark:shadow-gray-900 ${
          isReversed ? 'transform rotate-180' : ''
        } transition-shadow duration-300 ${glowColor} hover:shadow-lg cursor-pointer relative overflow-hidden`}
        whileHover={{ scale: 1.02, transition: { duration: 0.15 } }}
        whileTap={{ scale: 0.98 }}
        style={{ perspective: 1000 }}
      >
        {/* Shine effect on hover */}
        <motion.div
          className="absolute inset-0 bg-gradient-to-tr from-transparent via-white dark:via-gray-600 to-transparent opacity-0 hover:opacity-10 transition-opacity pointer-events-none z-20"
          initial={{ x: '-100%' }}
          whileHover={{ x: '100%' }}
          transition={{ duration: 0.6 }}
        />

        {/* Card image or fallback */}
        {shouldShowImage ? (
          <>
            <img
              src={imagePath}
              alt={showBack ? 'Card back' : card.name}
              className={`w-full h-full object-cover rounded-md ${!imageLoaded ? 'opacity-0' : 'opacity-100'} transition-opacity duration-300`}
              onLoad={() => setImageLoaded(true)}
              onError={() => setImageError(true)}
            />
            {/* Loading skeleton */}
            {!imageLoaded && (
              <div className="absolute inset-0 bg-gradient-to-r from-gray-200 via-gray-300 to-gray-200 dark:from-gray-700 dark:via-gray-600 dark:to-gray-700 animate-pulse" />
            )}
          </>
        ) : (
          /* Fallback to text display if image fails or unavailable */
          <div className={`flex flex-col items-center justify-center p-3 h-full ${isReversed ? 'transform rotate-180' : ''} relative z-10`}>
            <div className="text-xs text-gray-500 dark:text-gray-400 mb-2">{card.arcanaType.name}</div>
            <div className="text-sm font-semibold text-gray-900 dark:text-gray-100 leading-tight text-center">{card.name}</div>
          </div>
        )}
      </motion.div>
      {position !== undefined && (
        <motion.div
          className="mt-2 text-xs text-gray-500 dark:text-gray-400"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: delay + 0.2 }}
        >
          Position {position}
        </motion.div>
      )}
      {isReversed && (
        <motion.div
          className="mt-1 text-xs text-red-600 dark:text-red-400 font-medium"
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: delay + 0.3, type: 'spring', stiffness: 300 }}
        >
          Reversed
        </motion.div>
      )}
    </motion.div>
  );
}

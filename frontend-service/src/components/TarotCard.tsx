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
  const [isHovered, setIsHovered] = useState(false);

  const sizeClasses = {
    small: 'w-20 h-32',
    medium: 'w-32 h-48',
    large: 'w-40 h-60',
  };

  const isMajorArcana = card.arcanaType.name === 'MAJOR';
  const arcanaColor = isMajorArcana ? 'border-cosmic-500' : 'border-mystic-500';
  const glowColor = isMajorArcana ? 'shadow-cosmic' : 'shadow-mystic';

  const imagePath = showBack ? CARD_BACK_IMAGE : getCardImagePath(card.id);
  const shouldShowImage = imagePath && !imageError;

  return (
    <motion.div
      className="flex flex-col items-center group"
      variants={fadeInUp}
      initial="hidden"
      animate="visible"
      transition={{ delay }}
    >
      <motion.div
        className={`${sizeClasses[size]} border-3 ${arcanaColor} rounded-xl ${
          isReversed ? 'transform rotate-180' : ''
        } transition-all duration-500 ${glowColor} cursor-pointer relative overflow-hidden`}
        style={{
          perspective: 1000,
          backgroundColor: 'rgba(3, 7, 18, 0.8)',
        }}
        whileHover={{
          scale: 1.08,
          rotateY: isHovered ? 5 : 0,
          transition: { duration: 0.3 }
        }}
        whileTap={{ scale: 0.95 }}
        onHoverStart={() => setIsHovered(true)}
        onHoverEnd={() => setIsHovered(false)}
        animate={isHovered ? {
          boxShadow: isMajorArcana
            ? '0 0 40px rgba(217, 70, 239, 0.6), 0 0 80px rgba(217, 70, 239, 0.3)'
            : '0 0 40px rgba(139, 92, 246, 0.6), 0 0 80px rgba(139, 92, 246, 0.3)'
        } : {}}
      >
        {/* Mystical particles effect on hover */}
        {isHovered && (
          <motion.div
            className="absolute inset-0 pointer-events-none z-30"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            {[...Array(8)].map((_, i) => (
              <motion.div
                key={i}
                className={`absolute w-1 h-1 rounded-full ${isMajorArcana ? 'bg-cosmic-400' : 'bg-mystic-400'}`}
                style={{
                  left: `${Math.random() * 100}%`,
                  top: `${Math.random() * 100}%`,
                }}
                animate={{
                  y: [0, -20, -40],
                  opacity: [0, 1, 0],
                  scale: [0, 1, 0],
                }}
                transition={{
                  duration: 1.5,
                  repeat: Infinity,
                  delay: i * 0.15,
                }}
              />
            ))}
          </motion.div>
        )}

        {/* Enhanced shine effect */}
        <motion.div
          className="absolute inset-0 pointer-events-none z-20"
          style={{
            background: isMajorArcana
              ? 'linear-gradient(135deg, transparent 30%, rgba(217, 70, 239, 0.3) 50%, transparent 70%)'
              : 'linear-gradient(135deg, transparent 30%, rgba(139, 92, 246, 0.3) 50%, transparent 70%)',
          }}
          initial={{ x: '-100%', opacity: 0 }}
          animate={isHovered ? { x: '100%', opacity: 1 } : { x: '-100%', opacity: 0 }}
          transition={{ duration: 0.8 }}
        />

        {/* Card image or fallback */}
        {shouldShowImage ? (
          <>
            <img
              src={imagePath}
              alt={showBack ? 'Card back' : card.name}
              className={`w-full h-full object-cover rounded-lg ${!imageLoaded ? 'opacity-0' : 'opacity-100'} transition-opacity duration-500 relative z-10`}
              onLoad={() => setImageLoaded(true)}
              onError={() => setImageError(true)}
            />
            {/* Loading skeleton with mystical shimmer */}
            {!imageLoaded && (
              <div className="absolute inset-0 rounded-lg overflow-hidden">
                <div className="absolute inset-0 bg-gradient-to-r from-void-900 via-mystic-900 to-void-900 animate-pulse" />
                <motion.div
                  className="absolute inset-0"
                  style={{ background: 'linear-gradient(to right, transparent, rgba(124, 58, 237, 0.3), transparent)' }}
                  animate={{ x: ['-100%', '100%'] }}
                  transition={{ duration: 1.5, repeat: Infinity, ease: 'linear' }}
                />
              </div>
            )}
          </>
        ) : (
          /* Fallback to text display with mystical styling */
          <div className={`flex flex-col items-center justify-center p-4 h-full ${isReversed ? 'transform rotate-180' : ''} relative z-10`}>
            <div className={`text-xs font-accent mb-3 px-3 py-1 rounded-full ${isMajorArcana ? 'text-cosmic-400' : 'text-mystic-400'}`} style={{ backgroundColor: isMajorArcana ? 'rgba(112, 26, 117, 0.5)' : 'rgba(76, 29, 149, 0.5)' }}>
              {card.arcanaType.name}
            </div>
            <div className="text-base font-display font-semibold text-gray-100 leading-snug text-center">
              {card.name}
            </div>
          </div>
        )}

        {/* Arcana type indicator glow */}
        <div className={`absolute top-2 right-2 w-3 h-3 rounded-full z-20 ${isMajorArcana ? 'bg-cosmic-400' : 'bg-mystic-400'}`} style={{ boxShadow: isMajorArcana ? '0 0 10px rgba(217, 70, 239, 0.8)' : '0 0 10px rgba(139, 92, 246, 0.8)' }}></div>
      </motion.div>

      {position !== undefined && (
        <motion.div
          className="mt-3 text-sm font-accent text-gray-400 group-hover:text-gold-400 transition-colors"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: delay + 0.2 }}
        >
          Position {position}
        </motion.div>
      )}
      {isReversed && (
        <motion.div
          className="mt-2 px-3 py-1 text-xs font-serif font-semibold text-red-300 rounded-full border"
          style={{ backgroundColor: 'rgba(127, 29, 29, 0.3)', borderColor: 'rgba(239, 68, 68, 0.5)' }}
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: delay + 0.3, type: 'spring', stiffness: 300 }}
        >
          ⟲ Reversed
        </motion.div>
      )}
    </motion.div>
  );
}

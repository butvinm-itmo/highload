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
  spreadFromDeck?: boolean;
  totalCards?: number;
}

export function TarotCard({ card, isReversed = false, position, size = 'medium', delay = 0, showBack = false, spreadFromDeck = false }: TarotCardProps) {
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

  // Deck spreading animation variants - cards fly in from top and settle into grid position
  const deckSpreadVariants = spreadFromDeck ? {
    hidden: {
      x: 300,
      y: -400,
      rotateZ: 45,
      opacity: 0,
      scale: 0.6,
    },
    visible: {
      x: 0,
      y: 0,
      rotateZ: 0,
      opacity: 1,
      scale: 1,
    }
  } : fadeInUp;

  return (
    <motion.div
      className="flex flex-col items-center group"
      variants={deckSpreadVariants}
      initial="hidden"
      animate="visible"
      transition={spreadFromDeck ? {
        delay,
        duration: 0.7,
        type: 'spring',
        stiffness: 120,
        damping: 18,
        mass: 0.8
      } : {
        delay
      }}
    >
      <motion.div
        className={`${sizeClasses[size]} border-3 ${
          isReversed ? 'border-red-500' : arcanaColor
        } rounded-xl transition-all duration-500 ${glowColor} cursor-pointer relative overflow-hidden`}
        style={{
          perspective: 1000,
          backgroundColor: isReversed ? 'rgba(20, 5, 5, 0.9)' : 'rgba(3, 7, 18, 0.8)',
        }}
        whileHover={{
          scale: 1.08,
          rotateY: isHovered ? 5 : 0,
          transition: { duration: 0.3 }
        }}
        whileTap={{ scale: 0.95 }}
        onHoverStart={() => setIsHovered(true)}
        onHoverEnd={() => setIsHovered(false)}
        animate={isReversed ? {
          boxShadow: [
            '0 0 20px rgba(220, 38, 38, 0.4), 0 0 40px rgba(220, 38, 38, 0.2)',
            '0 0 30px rgba(220, 38, 38, 0.6), 0 0 60px rgba(220, 38, 38, 0.3)',
            '0 0 20px rgba(220, 38, 38, 0.4), 0 0 40px rgba(220, 38, 38, 0.2)',
          ],
        } : isHovered ? {
          boxShadow: isMajorArcana
            ? '0 0 40px rgba(217, 70, 239, 0.6), 0 0 80px rgba(217, 70, 239, 0.3)'
            : '0 0 40px rgba(139, 92, 246, 0.6), 0 0 80px rgba(139, 92, 246, 0.3)'
        } : {}}
        transition={isReversed ? {
          boxShadow: {
            duration: 2,
            repeat: Infinity,
            ease: 'easeInOut'
          }
        } : {}}
      >
        {/* Reversed card dark energy effect (always visible) */}
        {isReversed && (
          <motion.div className="absolute inset-0 pointer-events-none z-30">
            {[...Array(12)].map((_, i) => (
              <motion.div
                key={`reversed-${i}`}
                className="absolute w-1.5 h-1.5 rounded-full bg-red-500"
                style={{
                  left: `${Math.random() * 100}%`,
                  top: '100%',
                  boxShadow: '0 0 8px rgba(220, 38, 38, 0.8)',
                }}
                animate={{
                  y: [0, -Math.random() * 150 - 100],
                  x: [0, (Math.random() - 0.5) * 40],
                  opacity: [0.8, 0.6, 0],
                  scale: [1, 0.8, 0],
                }}
                transition={{
                  duration: 2 + Math.random(),
                  repeat: Infinity,
                  delay: i * 0.2,
                  ease: 'easeOut',
                }}
              />
            ))}
            {/* Smoke effect for reversed cards */}
            {[...Array(3)].map((_, i) => (
              <motion.div
                key={`smoke-${i}`}
                className="absolute rounded-full"
                style={{
                  left: `${20 + i * 30}%`,
                  bottom: '-10%',
                  width: '40px',
                  height: '40px',
                  background: 'radial-gradient(circle, rgba(139, 0, 0, 0.4) 0%, transparent 70%)',
                  filter: 'blur(8px)',
                }}
                animate={{
                  y: [-10, -80, -150],
                  opacity: [0, 0.5, 0],
                  scale: [0.5, 1.5, 2],
                }}
                transition={{
                  duration: 3,
                  repeat: Infinity,
                  delay: i * 1,
                  ease: 'easeOut',
                }}
              />
            ))}
          </motion.div>
        )}

        {/* Mystical particles effect on hover for normal cards */}
        {isHovered && !isReversed && (
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
            background: isReversed
              ? 'linear-gradient(135deg, transparent 30%, rgba(220, 38, 38, 0.4) 50%, transparent 70%)'
              : isMajorArcana
              ? 'linear-gradient(135deg, transparent 30%, rgba(217, 70, 239, 0.3) 50%, transparent 70%)'
              : 'linear-gradient(135deg, transparent 30%, rgba(139, 92, 246, 0.3) 50%, transparent 70%)',
          }}
          initial={{ x: '-100%', opacity: 0 }}
          animate={isReversed ? { x: '100%', opacity: 0.8 } : isHovered ? { x: '100%', opacity: 1 } : { x: '-100%', opacity: 0 }}
          transition={isReversed ? { duration: 2, repeat: Infinity, repeatDelay: 1 } : { duration: 0.8 }}
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
          <div className="flex flex-col items-center justify-center p-4 h-full relative z-10">
            <div className={`text-xs font-accent mb-3 px-3 py-1 rounded-full ${isMajorArcana ? 'text-cosmic-400' : 'text-mystic-400'}`} style={{ backgroundColor: isMajorArcana ? 'rgba(112, 26, 117, 0.5)' : 'rgba(76, 29, 149, 0.5)' }}>
              {card.arcanaType.name}
            </div>
            <div className="text-base font-display font-semibold text-gray-100 leading-snug text-center">
              {card.name}
            </div>
          </div>
        )}

        {/* Arcana type indicator glow */}
        <motion.div
          className={`absolute top-2 right-2 w-3 h-3 rounded-full z-20 ${
            isReversed ? 'bg-red-500' : isMajorArcana ? 'bg-cosmic-400' : 'bg-mystic-400'
          }`}
          style={{
            boxShadow: isReversed
              ? '0 0 10px rgba(220, 38, 38, 0.8)'
              : isMajorArcana
              ? '0 0 10px rgba(217, 70, 239, 0.8)'
              : '0 0 10px rgba(139, 92, 246, 0.8)'
          }}
          animate={isReversed ? {
            scale: [1, 1.3, 1],
            opacity: [1, 0.7, 1],
          } : {}}
          transition={isReversed ? {
            duration: 1.5,
            repeat: Infinity,
            ease: 'easeInOut',
          } : {}}
        />
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

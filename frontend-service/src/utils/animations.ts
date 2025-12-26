import type { Variants } from 'framer-motion';

// Animation durations
export const duration = {
  fast: 0.15,
  normal: 0.3,
  slow: 0.5,
};

// Easing functions
export const easing = {
  smooth: [0.4, 0.0, 0.2, 1] as const,
  bounce: [0.68, -0.55, 0.265, 1.55] as const,
  spring: { type: 'spring', stiffness: 300, damping: 30 } as const,
};

// Fade animations
export const fadeIn: Variants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { duration: duration.normal } },
  exit: { opacity: 0, transition: { duration: duration.fast } },
};

export const fadeInUp: Variants = {
  hidden: { opacity: 0, y: 20 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: duration.normal, ease: easing.smooth }
  },
  exit: { opacity: 0, y: -10, transition: { duration: duration.fast } },
};

export const fadeInDown: Variants = {
  hidden: { opacity: 0, y: -20 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: duration.normal, ease: easing.smooth }
  },
  exit: { opacity: 0, y: 10, transition: { duration: duration.fast } },
};

// Scale animations
export const scaleIn: Variants = {
  hidden: { opacity: 0, scale: 0.9 },
  visible: {
    opacity: 1,
    scale: 1,
    transition: { duration: duration.normal, ease: easing.smooth }
  },
  exit: { opacity: 0, scale: 0.95, transition: { duration: duration.fast } },
};

export const scaleBounce: Variants = {
  hidden: { opacity: 0, scale: 0.8 },
  visible: {
    opacity: 1,
    scale: 1,
    transition: { duration: duration.normal, ease: easing.bounce }
  },
  exit: { opacity: 0, scale: 0.9, transition: { duration: duration.fast } },
};

// Slide animations
export const slideInRight: Variants = {
  hidden: { opacity: 0, x: 100 },
  visible: {
    opacity: 1,
    x: 0,
    transition: { duration: duration.normal, ease: easing.smooth }
  },
  exit: { opacity: 0, x: 50, transition: { duration: duration.fast } },
};

export const slideInLeft: Variants = {
  hidden: { opacity: 0, x: -100 },
  visible: {
    opacity: 1,
    x: 0,
    transition: { duration: duration.normal, ease: easing.smooth }
  },
  exit: { opacity: 0, x: -50, transition: { duration: duration.fast } },
};

// Modal/Backdrop animations
export const backdropFade: Variants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { duration: duration.fast } },
  exit: { opacity: 0, transition: { duration: duration.fast } },
};

export const modalScale: Variants = {
  hidden: { opacity: 0, scale: 0.95, y: 20 },
  visible: {
    opacity: 1,
    scale: 1,
    y: 0,
    transition: { duration: duration.normal, ease: easing.smooth }
  },
  exit: {
    opacity: 0,
    scale: 0.95,
    y: 10,
    transition: { duration: duration.fast }
  },
};

// Toast animations
export const toastSlideIn: Variants = {
  hidden: { opacity: 0, y: -50, scale: 0.95 },
  visible: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: { duration: duration.normal, ease: easing.smooth }
  },
  exit: {
    opacity: 0,
    y: -20,
    scale: 0.95,
    transition: { duration: duration.fast }
  },
};

// Stagger container
export const staggerContainer: Variants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1,
      delayChildren: 0.05,
    },
  },
};

export const staggerItem: Variants = {
  hidden: { opacity: 0, y: 20 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: duration.normal, ease: easing.smooth }
  },
};

// 3D Flip animation for Tarot cards
export const cardFlip: Variants = {
  front: {
    rotateY: 0,
    transition: { duration: duration.slow, ease: easing.smooth }
  },
  back: {
    rotateY: 180,
    transition: { duration: duration.slow, ease: easing.smooth }
  },
};

// Pulse animation for notifications
export const pulse = {
  scale: [1, 1.1, 1],
  transition: {
    duration: 0.6,
    repeat: 2,
    ease: easing.smooth,
  },
};

// Shake animation for errors
export const shake = {
  x: [0, -10, 10, -10, 10, 0],
  transition: {
    duration: 0.4,
    ease: easing.smooth,
  },
};

// Hover effects
export const hoverLift = {
  y: -4,
  boxShadow: '0 10px 20px rgba(0, 0, 0, 0.1)',
  transition: { duration: duration.fast, ease: easing.smooth },
};

export const hoverScale = {
  scale: 1.02,
  transition: { duration: duration.fast, ease: easing.smooth },
};

export const tapScale = {
  scale: 0.98,
  transition: { duration: 0.1 },
};

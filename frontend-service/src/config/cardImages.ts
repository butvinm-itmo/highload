/**
 * Card image mapping configuration
 * Maps card IDs to their corresponding image filenames
 */

// Major Arcana (0-21)
const MAJOR_ARCANA_CARDS = [
  { id: '40000000-0000-0000-0000-000000000000', name: 'The Fool', imageNumber: 0 },
  { id: '40000000-0000-0000-0000-000000000001', name: 'The Magician', imageNumber: 1 },
  { id: '40000000-0000-0000-0000-000000000002', name: 'The High Priestess', imageNumber: 2 },
  { id: '40000000-0000-0000-0000-000000000003', name: 'The Empress', imageNumber: 3 },
  { id: '40000000-0000-0000-0000-000000000004', name: 'The Emperor', imageNumber: 4 },
  { id: '40000000-0000-0000-0000-000000000005', name: 'The Hierophant', imageNumber: 5 },
  { id: '40000000-0000-0000-0000-000000000006', name: 'The Lovers', imageNumber: 6 },
  { id: '40000000-0000-0000-0000-000000000007', name: 'The Chariot', imageNumber: 7 },
  { id: '40000000-0000-0000-0000-000000000008', name: 'Strength', imageNumber: 8 },
  { id: '40000000-0000-0000-0000-000000000009', name: 'The Hermit', imageNumber: 9 },
  { id: '40000000-0000-0000-0000-000000000010', name: 'Wheel of Fortune', imageNumber: 10 },
  { id: '40000000-0000-0000-0000-000000000011', name: 'Justice', imageNumber: 11 },
  { id: '40000000-0000-0000-0000-000000000012', name: 'The Hanged Man', imageNumber: 12 },
  { id: '40000000-0000-0000-0000-000000000013', name: 'Death', imageNumber: 13 },
  { id: '40000000-0000-0000-0000-000000000014', name: 'Temperance', imageNumber: 14 },
  { id: '40000000-0000-0000-0000-000000000015', name: 'The Devil', imageNumber: 15 },
  { id: '40000000-0000-0000-0000-000000000016', name: 'The Tower', imageNumber: 16 },
  { id: '40000000-0000-0000-0000-000000000017', name: 'The Star', imageNumber: 17 },
  { id: '40000000-0000-0000-0000-000000000018', name: 'The Moon', imageNumber: 18 },
  { id: '40000000-0000-0000-0000-000000000019', name: 'The Sun', imageNumber: 19 },
  { id: '40000000-0000-0000-0000-000000000020', name: 'Judgement', imageNumber: 20 },
  { id: '40000000-0000-0000-0000-000000000021', name: 'The World', imageNumber: 21 },
];

// Minor Arcana - Wands (22-35)
const WANDS_CARDS = [
  { id: '41000000-0000-0000-0000-000000000001', name: 'Ace of Wands', imageNumber: 22 },
  { id: '41000000-0000-0000-0000-000000000002', name: 'Two of Wands', imageNumber: 23 },
  { id: '41000000-0000-0000-0000-000000000003', name: 'Three of Wands', imageNumber: 24 },
  { id: '41000000-0000-0000-0000-000000000004', name: 'Four of Wands', imageNumber: 25 },
  { id: '41000000-0000-0000-0000-000000000005', name: 'Five of Wands', imageNumber: 26 },
  { id: '41000000-0000-0000-0000-000000000006', name: 'Six of Wands', imageNumber: 27 },
  { id: '41000000-0000-0000-0000-000000000007', name: 'Seven of Wands', imageNumber: 28 },
  { id: '41000000-0000-0000-0000-000000000008', name: 'Eight of Wands', imageNumber: 29 },
  { id: '41000000-0000-0000-0000-000000000009', name: 'Nine of Wands', imageNumber: 30 },
  { id: '41000000-0000-0000-0000-000000000010', name: 'Ten of Wands', imageNumber: 31 },
  { id: '41000000-0000-0000-0000-000000000011', name: 'Page of Wands', imageNumber: 32 },
  { id: '41000000-0000-0000-0000-000000000012', name: 'Knight of Wands', imageNumber: 33 },
  { id: '41000000-0000-0000-0000-000000000013', name: 'Queen of Wands', imageNumber: 34 },
  { id: '41000000-0000-0000-0000-000000000014', name: 'King of Wands', imageNumber: 35 },
];

// Minor Arcana - Cups (36-49)
const CUPS_CARDS = [
  { id: '42000000-0000-0000-0000-000000000001', name: 'Ace of Cups', imageNumber: 36 },
  { id: '42000000-0000-0000-0000-000000000002', name: 'Two of Cups', imageNumber: 37 },
  { id: '42000000-0000-0000-0000-000000000003', name: 'Three of Cups', imageNumber: 38 },
  { id: '42000000-0000-0000-0000-000000000004', name: 'Four of Cups', imageNumber: 39 },
  { id: '42000000-0000-0000-0000-000000000005', name: 'Five of Cups', imageNumber: 40 },
  { id: '42000000-0000-0000-0000-000000000006', name: 'Six of Cups', imageNumber: 41 },
  { id: '42000000-0000-0000-0000-000000000007', name: 'Seven of Cups', imageNumber: 42 },
  { id: '42000000-0000-0000-0000-000000000008', name: 'Eight of Cups', imageNumber: 43 },
  { id: '42000000-0000-0000-0000-000000000009', name: 'Nine of Cups', imageNumber: 44 },
  { id: '42000000-0000-0000-0000-000000000010', name: 'Ten of Cups', imageNumber: 45 },
  { id: '42000000-0000-0000-0000-000000000011', name: 'Page of Cups', imageNumber: 46 },
  { id: '42000000-0000-0000-0000-000000000012', name: 'Knight of Cups', imageNumber: 47 },
  { id: '42000000-0000-0000-0000-000000000013', name: 'Queen of Cups', imageNumber: 48 },
  { id: '42000000-0000-0000-0000-000000000014', name: 'King of Cups', imageNumber: 49 },
];

// Minor Arcana - Swords (50-63)
const SWORDS_CARDS = [
  { id: '43000000-0000-0000-0000-000000000001', name: 'Ace of Swords', imageNumber: 50 },
  { id: '43000000-0000-0000-0000-000000000002', name: 'Two of Swords', imageNumber: 51 },
  { id: '43000000-0000-0000-0000-000000000003', name: 'Three of Swords', imageNumber: 52 },
  { id: '43000000-0000-0000-0000-000000000004', name: 'Four of Swords', imageNumber: 53 },
  { id: '43000000-0000-0000-0000-000000000005', name: 'Five of Swords', imageNumber: 54 },
  { id: '43000000-0000-0000-0000-000000000006', name: 'Six of Swords', imageNumber: 55 },
  { id: '43000000-0000-0000-0000-000000000007', name: 'Seven of Swords', imageNumber: 56 },
  { id: '43000000-0000-0000-0000-000000000008', name: 'Eight of Swords', imageNumber: 57 },
  { id: '43000000-0000-0000-0000-000000000009', name: 'Nine of Swords', imageNumber: 58 },
  { id: '43000000-0000-0000-0000-000000000010', name: 'Ten of Swords', imageNumber: 59 },
  { id: '43000000-0000-0000-0000-000000000011', name: 'Page of Swords', imageNumber: 60 },
  { id: '43000000-0000-0000-0000-000000000012', name: 'Knight of Swords', imageNumber: 61 },
  { id: '43000000-0000-0000-0000-000000000013', name: 'Queen of Swords', imageNumber: 62 },
  { id: '43000000-0000-0000-0000-000000000014', name: 'King of Swords', imageNumber: 63 },
];

// Minor Arcana - Pentacles (64-77)
const PENTACLES_CARDS = [
  { id: '44000000-0000-0000-0000-000000000001', name: 'Ace of Pentacles', imageNumber: 64 },
  { id: '44000000-0000-0000-0000-000000000002', name: 'Two of Pentacles', imageNumber: 65 },
  { id: '44000000-0000-0000-0000-000000000003', name: 'Three of Pentacles', imageNumber: 66 },
  { id: '44000000-0000-0000-0000-000000000004', name: 'Four of Pentacles', imageNumber: 67 },
  { id: '44000000-0000-0000-0000-000000000005', name: 'Five of Pentacles', imageNumber: 68 },
  { id: '44000000-0000-0000-0000-000000000006', name: 'Six of Pentacles', imageNumber: 69 },
  { id: '44000000-0000-0000-0000-000000000007', name: 'Seven of Pentacles', imageNumber: 70 },
  { id: '44000000-0000-0000-0000-000000000008', name: 'Eight of Pentacles', imageNumber: 71 },
  { id: '44000000-0000-0000-0000-000000000009', name: 'Nine of Pentacles', imageNumber: 72 },
  { id: '44000000-0000-0000-0000-000000000010', name: 'Ten of Pentacles', imageNumber: 73 },
  { id: '44000000-0000-0000-0000-000000000011', name: 'Page of Pentacles', imageNumber: 74 },
  { id: '44000000-0000-0000-0000-000000000012', name: 'Knight of Pentacles', imageNumber: 75 },
  { id: '44000000-0000-0000-0000-000000000013', name: 'Queen of Pentacles', imageNumber: 76 },
  { id: '44000000-0000-0000-0000-000000000014', name: 'King of Pentacles', imageNumber: 77 },
];

// Combine all cards into a single array
const ALL_CARDS = [
  ...MAJOR_ARCANA_CARDS,
  ...WANDS_CARDS,
  ...CUPS_CARDS,
  ...SWORDS_CARDS,
  ...PENTACLES_CARDS,
];

// Create a map for O(1) lookup by card ID
const CARD_IMAGE_MAP = new Map(
  ALL_CARDS.map((card) => [card.id, card.imageNumber])
);

/**
 * Get the image filename for a card by its ID
 * @param cardId - The UUID of the card
 * @returns The image filename (e.g., "00.jpg") or null if not found
 */
export function getCardImagePath(cardId: string): string | null {
  const imageNumber = CARD_IMAGE_MAP.get(cardId);
  if (imageNumber === undefined) {
    return null;
  }
  const paddedNumber = imageNumber.toString().padStart(2, '0');
  return `/tarot-cards/${paddedNumber}.jpg`;
}

/**
 * Get the card back image path
 */
export const CARD_BACK_IMAGE = '/tarot-cards/back.jpg';

/**
 * Get all card image paths for preloading
 */
export function getAllCardImagePaths(): string[] {
  return Array.from({ length: 78 }, (_, i) => {
    const paddedNumber = i.toString().padStart(2, '0');
    return `/tarot-cards/${paddedNumber}.jpg`;
  });
}

/**
 * Preload all card images for better performance
 */
export function preloadCardImages(): Promise<void[]> {
  const images = getAllCardImagePaths();
  const promises = images.map((src) => {
    return new Promise<void>((resolve, reject) => {
      const img = new Image();
      img.onload = () => resolve();
      img.onerror = () => reject(new Error(`Failed to load ${src}`));
      img.src = src;
    });
  });
  return Promise.all(promises);
}

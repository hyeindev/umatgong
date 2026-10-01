import { useEffect, useState } from 'react';

/** 값이 delayMs 동안 바뀌지 않으면 그때 내보낸다. 지도 이동이 멈춘 뒤 조회하는 데 쓴다 */
export const useDebouncedValue = <T>(value: T, delayMs: number) => {
  const [settled, setSettled] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setSettled(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);
  return settled;
};

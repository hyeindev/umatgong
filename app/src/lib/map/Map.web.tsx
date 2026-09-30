import { View } from 'react-native';

import type { MapComponent } from './Map.types';

// TODO: 카카오맵 JS SDK로 구현한다. 지금은 자리만 차지하는 빈 뷰다.
export const Map: MapComponent = ({ style }) => <View style={style} />;

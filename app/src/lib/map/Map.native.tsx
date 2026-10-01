import { View } from 'react-native';

import type { MapComponent } from './Map.types';

// TODO: 카카오맵 네이티브 SDK로 구현한다. 지금은 자리만 차지하는 빈 뷰다 (웹 먼저).
// markers·onRegionChange는 아직 무시한다. 영역을 알리지 않으므로 화면은 핀을 조회하지 않는다.
export const Map: MapComponent = ({ style }) => <View style={style} />;

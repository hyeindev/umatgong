import { useLocalSearchParams, useRouter } from 'expo-router';
import { useMemo, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import Svg, { Path } from 'react-native-svg';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { usePlaceVisits } from '@/features/map';
import {
  BottomSheet,
  FriendSignalCard,
  PhotoGallery,
  RatingChip,
  VisitItem,
  summarizeDetail,
  useScrapStatus,
  useToggleScrap,
  useUpdateRating,
} from '@/features/place';
import { RatingPicker } from '@/features/record';
import { isApiError } from '@/lib/api';
import { directions, type DirectionsApp } from '@/lib/directions';
import { colors, layout, shape, size, spacing, textStyles } from '@/theme';
import type { Rating } from '@/types/visit';

// 장소 상세 (화면기획서 4.4, 시안 ref-all.html C1).
// 위에서부터: 사진 갤러리 → 가게 정보 → 핵심 신호(친구 평가) → 방문 기록. 아래에 「길찾기」·「가고싶다」 고정.
export default function PlaceDetailScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const params = useLocalSearchParams<{ placeId: string }>();
  const placeId = Number(params.placeId);
  const visits = usePlaceVisits(Number.isFinite(placeId) ? placeId : null);
  const detail = useMemo(() => (visits.data ? summarizeDetail(visits.data) : null), [visits.data]);
  const scrap = useScrapStatus(placeId);
  const toggleScrap = useToggleScrap(placeId);
  const updateRating = useUpdateRating();
  const [sheet, setSheet] = useState<'directions' | 'rating' | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const goBack = () => (router.canGoBack() ? router.back() : router.replace('/'));

  if (visits.isPending) {
    return (
      <View style={[styles.screen, styles.center]}>
        <ActivityIndicator color={colors.accent.default} />
      </View>
    );
  }

  // 내게 보이는 기록이 없는 장소(다른 클럽, 지워진 기록)는 없는 장소와 같게 보여 준다
  if (!detail) {
    const missing = visits.isError && !isApiError(visits.error, 'PLACE_NOT_FOUND');
    return (
      <View style={[styles.screen, styles.center, { paddingTop: insets.top }]}>
        <Text style={styles.body}>
          {missing
            ? '기록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'
            : '볼 수 있는 기록이 없어요.'}
        </Text>
        <Pressable accessibilityRole="button" onPress={goBack} style={styles.textButton}>
          <Text style={styles.textButtonLabel}>지도로 돌아가기</Text>
        </Pressable>
      </View>
    );
  }

  const { place, club, otherClubCount, photos, signal, myLatest, timeline } = detail;
  const scrapped = scrap.data?.scrapped ?? false;
  const meta = [place.category, place.address].filter(Boolean).join(' · ');

  const openDirections = async (app: DirectionsApp) => {
    setSheet(null);
    const opened = await directions.open(app, { name: place.name, coordinate: place.coordinate });
    if (!opened) {
      setNotice('지도앱을 열지 못했어요.');
    }
  };

  const changeRating = (rating: Rating) => {
    if (!myLatest) {
      return;
    }
    updateRating.mutate(
      { visitId: myLatest.id, rating },
      {
        onSuccess: () => setSheet(null),
        onError: () => setNotice('평가를 바꾸지 못했어요. 다시 시도해 주세요.'),
      },
    );
  };

  return (
    <View style={styles.screen}>
      <ScrollView
        contentContainerStyle={{ paddingBottom: size.button + spacing[12] + insets.bottom }}
      >
        <View style={{ paddingTop: insets.top }}>
          <PhotoGallery photos={photos} />
        </View>

        <View style={styles.content}>
          <View style={styles.info}>
            {myLatest ? (
              <View style={styles.mine}>
                <Text style={styles.mineLabel}>나도 갔던 곳</Text>
                <RatingChip rating={myLatest.rating} />
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="내 평가 고치기"
                  onPress={() => setSheet('rating')}
                  style={({ pressed }) => [styles.mineEdit, pressed && styles.pressed]}
                >
                  <Text style={styles.mineEditLabel}>평가 수정</Text>
                </Pressable>
              </View>
            ) : null}
            <Text style={styles.name} accessibilityRole="header">
              {place.name}
            </Text>
            {meta ? <Text style={styles.body}>{meta}</Text> : null}
            {club ? (
              <View style={styles.clubRow}>
                <View style={[styles.clubDot, { backgroundColor: colors.club[club.color] }]} />
                <Text style={styles.body}>
                  {club.name}
                  {otherClubCount > 0 ? ` 외 ${otherClubCount}개 클럽` : ''}
                </Text>
              </View>
            ) : null}
          </View>

          {signal ? <FriendSignalCard signal={signal} /> : null}

          <View style={styles.section}>
            <View style={styles.sectionHead}>
              <Text style={styles.sectionTitle}>방문 기록</Text>
              <Text style={styles.sectionCount}>{timeline.length}번</Text>
            </View>
            {timeline.map((visit) => (
              <VisitItem key={visit.id} visit={visit} />
            ))}
          </View>
        </View>
      </ScrollView>

      {/* 위에 떠 있는 뒤로 가기 — 사진 위에서도 보이게 반투명 원 */}
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="뒤로"
        onPress={goBack}
        style={({ pressed }) => [
          styles.back,
          { top: insets.top + spacing[3] },
          pressed && styles.pressed,
        ]}
      >
        <Svg width={10} height={16} viewBox="0 0 10 16">
          <Path d="M8 2 2 8l6 6" stroke={colors.text.primary} strokeWidth={2.2} fill="none" />
        </Svg>
      </Pressable>
      {photos.length > 0 ? (
        <View style={[styles.count, { top: insets.top + spacing[3] }]} pointerEvents="none">
          <Text style={styles.countLabel}>사진 {photos.length}장</Text>
        </View>
      ) : null}

      {notice ? (
        <Pressable
          accessibilityRole="alert"
          onPress={() => setNotice(null)}
          style={[styles.notice, { bottom: insets.bottom + size.button + spacing[8] }]}
        >
          <Text style={styles.noticeLabel}>{notice}</Text>
        </Pressable>
      ) : null}

      {/* 하단 고정 (화면기획서 4.4) */}
      <View style={[styles.bar, { paddingBottom: insets.bottom + spacing[5] }]}>
        <Pressable
          accessibilityRole="button"
          onPress={() => setSheet('directions')}
          style={({ pressed }) => [styles.barButton, styles.directions, pressed && styles.pressed]}
        >
          <Text style={styles.directionsLabel}>길찾기</Text>
        </Pressable>
        <Pressable
          accessibilityRole="switch"
          accessibilityLabel="가고싶다"
          accessibilityState={{ checked: scrapped, busy: scrap.isPending }}
          disabled={scrap.isPending}
          onPress={() =>
            toggleScrap.mutate(!scrapped, {
              onError: () => setNotice('저장하지 못했어요. 다시 시도해 주세요.'),
            })
          }
          style={({ pressed }) => [
            styles.barButton,
            styles.scrap,
            scrapped && styles.scrapOn,
            pressed && styles.pressed,
          ]}
        >
          <Svg width={14} height={17} viewBox="0 0 15 18">
            <Path
              d="M2 2.6h11v13.2l-5.5-4-5.5 4V2.6z"
              fill={scrapped ? colors.accent.default : colors.accent.on}
              stroke={scrapped ? colors.accent.default : colors.accent.on}
              strokeWidth={1.6}
              strokeLinejoin="round"
            />
          </Svg>
          <Text style={[styles.scrapLabel, scrapped && styles.scrapLabelOn]}>
            {scrapped ? '가고싶은 곳' : '가고싶다'}
          </Text>
        </Pressable>
      </View>

      {sheet === 'directions' ? (
        <BottomSheet title="어느 앱으로 길찾을까요?" onClose={() => setSheet(null)}>
          <SheetButton label="카카오맵" onPress={() => openDirections('kakao')} />
          <SheetButton label="네이버 지도" onPress={() => openDirections('naver')} />
        </BottomSheet>
      ) : null}

      {sheet === 'rating' && myLatest ? (
        <BottomSheet title="내 평가 고치기" onClose={() => setSheet(null)}>
          <RatingPicker value={myLatest.rating} onChange={changeRating} />
          {updateRating.isPending ? <ActivityIndicator color={colors.accent.default} /> : null}
        </BottomSheet>
      ) : null}
    </View>
  );
}

function SheetButton({ label, onPress }: { label: string; onPress: () => void }) {
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [styles.sheetButton, pressed && styles.pressed]}
    >
      <Text style={styles.sheetButtonLabel}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background.screen,
  },
  center: {
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing[3],
    paddingHorizontal: layout.screenGutter,
  },
  content: {
    paddingTop: spacing[6],
    paddingHorizontal: layout.screenGutter,
    gap: layout.sectionGap,
  },
  info: {
    gap: spacing[2],
  },
  mine: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    gap: spacing[2],
    paddingVertical: spacing[1.5],
    paddingLeft: spacing[3],
    paddingRight: spacing[1.5],
    marginBottom: spacing[1],
    borderRadius: shape.pill,
    backgroundColor: colors.surface.raised,
  },
  mineLabel: {
    ...textStyles.label,
    color: colors.text.primary,
  },
  mineEdit: {
    paddingVertical: spacing[1.5],
    paddingHorizontal: spacing[2.5],
    borderRadius: shape.pill,
    backgroundColor: colors.surface.sheet,
  },
  mineEditLabel: {
    ...textStyles.caption,
    color: colors.text.secondary,
  },
  name: {
    ...textStyles.display,
    color: colors.text.primary,
  },
  body: {
    ...textStyles.bodySmall,
    color: colors.text.secondary,
  },
  clubRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[1.5],
  },
  clubDot: {
    width: size.clubDot,
    height: size.clubDot,
    borderRadius: shape.pill,
  },
  section: {
    gap: spacing[5],
  },
  sectionHead: {
    flexDirection: 'row',
    alignItems: 'baseline',
    justifyContent: 'space-between',
  },
  sectionTitle: {
    ...textStyles.itemTitle,
    color: colors.text.primary,
  },
  sectionCount: {
    ...textStyles.meta,
    color: colors.text.tertiary,
  },
  back: {
    position: 'absolute',
    left: layout.screenGutter,
    width: size.floatingButton,
    height: size.floatingButton,
    borderRadius: shape.pill,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surface.onPhoto,
  },
  count: {
    position: 'absolute',
    right: layout.screenGutter,
    height: size.floatingButton,
    paddingHorizontal: spacing[3.5],
    borderRadius: shape.pill,
    justifyContent: 'center',
    backgroundColor: colors.surface.onPhoto,
  },
  countLabel: {
    ...textStyles.label,
    color: colors.text.primary,
  },
  notice: {
    position: 'absolute',
    left: layout.screenGutter,
    right: layout.screenGutter,
    padding: spacing[3],
    borderRadius: shape.button,
    backgroundColor: colors.surface.floating,
  },
  noticeLabel: {
    ...textStyles.bodySmall,
    color: colors.text.primary,
  },
  bar: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    flexDirection: 'row',
    gap: spacing[2],
    paddingTop: spacing[3.5],
    paddingHorizontal: layout.screenGutter,
    backgroundColor: colors.background.screen,
  },
  barButton: {
    height: size.button,
    borderRadius: shape.button,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing[2],
  },
  directions: {
    flex: 1,
    backgroundColor: colors.surface.cream,
  },
  directionsLabel: {
    ...textStyles.button,
    color: colors.text.onCream.primary,
  },
  scrap: {
    flex: 1.3,
    backgroundColor: colors.accent.default,
  },
  // 이미 저장했으면 라임 면을 줄이고 테두리만 — 한 번 더 누르면 풀린다는 상태가 보이게
  scrapOn: {
    backgroundColor: colors.surface.sheet,
    borderWidth: 1.5,
    borderColor: colors.accent.default,
  },
  scrapLabel: {
    ...textStyles.button,
    color: colors.accent.on,
  },
  scrapLabelOn: {
    color: colors.accent.default,
  },
  textButton: {
    padding: spacing[3],
  },
  textButtonLabel: {
    ...textStyles.label,
    color: colors.text.secondary,
    textDecorationLine: 'underline',
  },
  sheetButton: {
    height: size.button,
    borderRadius: shape.button,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surface.raised,
  },
  sheetButtonLabel: {
    ...textStyles.button,
    color: colors.text.primary,
  },
  pressed: {
    opacity: 0.85,
  },
});

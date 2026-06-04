import { describe, expect, it } from 'vitest';

import {
  allowLongStayLabel,
  auditStatusLabel,
  crowdLevelLabel,
  noiseLevelLabel,
  sceneLabel,
  trustLevelLabel,
  workspaceStatusLabel
} from './displayLabels';

describe('display labels', () => {
  it('translates place and profile system values into user-facing labels', () => {
    expect(workspaceStatusLabel('APPROVED')).toBe('已收录');
    expect(workspaceStatusLabel('CANDIDATE')).toBe('待完善');
    expect(trustLevelLabel('LOW')).toBe('资料较少');
    expect(auditStatusLabel('PENDING')).toBe('审核中');
  });

  it('translates review and checkin enum values into readable labels', () => {
    expect(sceneLabel('SELF_STUDY')).toBe('自习');
    expect(sceneLabel('READING')).toBe('阅读');
    expect(crowdLevelLabel('CROWDED')).toBe('较拥挤');
    expect(noiseLevelLabel('RELATIVELY_QUIET')).toBe('较安静');
    expect(allowLongStayLabel('TRUE')).toBe('适合久坐');
  });

  it('falls back gracefully for empty or unknown values', () => {
    expect(workspaceStatusLabel(null)).toBe('候选');
    expect(sceneLabel('CUSTOM_SCENE')).toBe('CUSTOM_SCENE');
  });
});

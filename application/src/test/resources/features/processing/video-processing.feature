Feature: Video Processing

  Scenario: Successfully process a valid video
    Given a video-uploaded event with videoId "550e8400-e29b-41d4-a716-446655440000" and userId "user-123"
    When the processing service handles the event
    Then the result status should be "DONE"
    And the frameCount should be greater than 0
    And a VIDEO_PROCESSED event should be published

  Scenario: Handle ffmpeg failure gracefully
    Given a video-uploaded event with videoId "660e8400-e29b-41d4-a716-446655440000" and userId "user-456"
    And the ffmpeg process will fail
    When the processing service handles the event
    Then the result status should be "FAILED"
    And a VIDEO_FAILED event should be published
    And no VIDEO_PROCESSED event should be published

  Scenario: List recent jobs
    Given the processing service has completed some jobs
    When a request is made to GET /api/jobs
    Then the response should contain the job list
    And the response status should be 200

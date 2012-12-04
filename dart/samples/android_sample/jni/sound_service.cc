#include "jni/sound_service.h"

#include "bin/log.h"
#include "jni/resource.h"

SoundService::SoundService(android_app* application)
    : application_(application),
      engine_(NULL),
      engine_if_(NULL),
      output_mix_(NULL),
      background_player_(NULL),
      background_player_if_(NULL),
      background_player_seek_if_(NULL) {
}

int32_t SoundService::Start() {
  Log::Print("Starting SoundService");

  const SLInterfaceID k_engine_mix_IIDs[] = { SL_IID_ENGINE };
  const SLboolean k_engine_mix_reqs[] = { SL_BOOLEAN_TRUE };
  const SLInterfaceID k_output_mix_IIDs[] = {};
  const SLboolean k_output_mix_reqs[] = {};
  if (slCreateEngine(&engine_, 0, NULL, 1, k_engine_mix_IIDs, k_engine_mix_reqs)
      == SL_RESULT_SUCCESS &&
      (*engine_)->Realize(engine_, SL_BOOLEAN_FALSE) == SL_RESULT_SUCCESS &&
      (*engine_)->GetInterface(engine_, SL_IID_ENGINE, &engine_if_) ==
          SL_RESULT_SUCCESS &&
      (*engine_if_)->CreateOutputMix(engine_if_, &output_mix_, 0,
                                    k_output_mix_IIDs, k_output_mix_reqs) ==
          SL_RESULT_SUCCESS &&
      (*output_mix_)->Realize(output_mix_, SL_BOOLEAN_FALSE) ==
          SL_RESULT_SUCCESS) {
    return 0;
  }
  Log::Print("Failed to start SoundService");
  Stop();
  return -1;
}

void SoundService::Stop() {
  StopBackground();
  if (output_mix_ != NULL) {
    (*output_mix_)->Destroy(output_mix_);
    output_mix_ = NULL;
  }
  if (engine_ != NULL) {
    (*engine_)->Destroy(engine_);
    engine_ = NULL;
    engine_if_ = NULL;
  }
}

int32_t SoundService::PlayBackground(const char* path) {
  Resource resource(application_, path);
  if (resource.open() < 0) {
    Log::Print("Could not open file %s", path);
    return -1;
  }
  Log::Print("Saving FD data");
  SLDataLocator_AndroidFD data_locator_in;
  data_locator_in.locatorType = SL_DATALOCATOR_ANDROIDFD;
  data_locator_in.fd = resource.descriptor();
  data_locator_in.offset = resource.start();
  data_locator_in.length = resource.length();
  resource.close();

  Log::Print("Init data format");
  SLDataFormat_MIME data_format;
  data_format.formatType = SL_DATAFORMAT_MIME;
  data_format.mimeType = NULL;
  data_format.containerType = SL_CONTAINERTYPE_UNSPECIFIED;

  Log::Print("Init data source");
  SLDataSource data_source;
  data_source.pLocator = &data_locator_in;
  data_source.pFormat = &data_format;

  Log::Print("Init out locator");
  SLDataLocator_OutputMix data_locator_out;
  data_locator_out.locatorType = SL_DATALOCATOR_OUTPUTMIX;
  data_locator_out.outputMix = output_mix_;

  Log::Print("Init data sink");
  SLDataSink data_sink;
  data_sink.pLocator = &data_locator_out;
  data_sink.pFormat = NULL;

  const SLInterfaceID k_background_player_IIDs[] = { SL_IID_PLAY, SL_IID_SEEK };
  const SLboolean k_background_player_reqs[] =
      { SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE };

  Log::Print("Creating audio player");
  if ((*engine_if_)->
          CreateAudioPlayer(engine_if_, &background_player_,
                            &data_source, &data_sink, 2,
                            k_background_player_IIDs,
                            k_background_player_reqs) != SL_RESULT_SUCCESS) {
    Log::PrintErr("Couldn't create audio player");
    return -1;
  }
  Log::Print("Created audio player");
  if ((*background_player_)->
          Realize(background_player_, SL_BOOLEAN_FALSE) != SL_RESULT_SUCCESS) {
    Log::PrintErr("Couldn't realize audio player");
    return -1;
  }
  Log::Print("Realized audio player");
  if ((*background_player_)->
          GetInterface(background_player_, SL_IID_PLAY,
                       &background_player_if_) != SL_RESULT_SUCCESS) {
    Log::PrintErr("Couldn't get player interface");
    return -1;
  }
  Log::Print("Got player interface");
  if ((*background_player_)->
          GetInterface(background_player_, SL_IID_SEEK,
                       &background_player_seek_if_) != SL_RESULT_SUCCESS) {
    Log::PrintErr("Couldn't get seek interface");
    return -1;
  }
  Log::Print("Got seek interface");
  if ((*background_player_seek_if_)->
          SetLoop(background_player_seek_if_, SL_BOOLEAN_TRUE, 0,
                  SL_TIME_UNKNOWN) != SL_RESULT_SUCCESS) {
    Log::PrintErr("Couldn't set loop");
    return -1;
  }
  Log::Print("Set loop");
  if ((*background_player_if_)->
          SetPlayState(background_player_if_, SL_PLAYSTATE_PLAYING) !=
          SL_RESULT_SUCCESS) {
    Log::PrintErr("Couldn't start playing");
    return -1;
  }
  Log::Print("Started playing");
  return 0;
}

void SoundService::StopBackground() {
  if (background_player_if_ != NULL) {
    SLuint32 state;
    (*background_player_)->GetState(background_player_, &state);
    if (state == SL_OBJECT_STATE_REALIZED) {
      (*background_player_if_)->SetPlayState(background_player_if_,
                                           SL_PLAYSTATE_PAUSED);

      (*background_player_)->Destroy(background_player_);
      background_player_ = NULL;
      background_player_if_ = NULL;
      background_player_seek_if_ = NULL;
    }
  }
}


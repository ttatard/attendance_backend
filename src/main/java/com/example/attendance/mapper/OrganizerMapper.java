package com.example.attendance.mapper;

import com.example.attendance.dto.OrganizerDto;
import com.example.attendance.entity.Organizer;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring",
        imports = {LocalDateTime.class})
public interface OrganizerMapper {

    OrganizerMapper INSTANCE = Mappers.getMapper(OrganizerMapper.class);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "createdAt", source = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "updatedAt", source = "updatedAt", dateFormat = "yyyy-MM-dd HH:mm:ss")
    OrganizerDto toDto(Organizer organizer);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "enrolledUsers", ignore = true)
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    Organizer toEntity(OrganizerDto organizerDto);

    @AfterMapping
    default void setDefaultActiveStatus(@MappingTarget Organizer organizer) {
        if (organizer.getIsActive() == null) {
            organizer.setIsActive(true);
        }
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "enrolledUsers", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateOrganizerFromDto(OrganizerDto dto, @MappingTarget Organizer entity);
}